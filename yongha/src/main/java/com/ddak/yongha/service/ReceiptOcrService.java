package com.ddak.yongha.service;

import static org.bytedeco.leptonica.global.lept.pixClone;
import static org.bytedeco.leptonica.global.lept.pixConvertTo8;
import static org.bytedeco.leptonica.global.lept.pixDestroy;
import static org.bytedeco.leptonica.global.lept.pixFindSkew;
import static org.bytedeco.leptonica.global.lept.pixOtsuAdaptiveThreshold;
import static org.bytedeco.leptonica.global.lept.pixReadMem;
import static org.bytedeco.leptonica.global.lept.pixRotateAMGray;
import static org.bytedeco.leptonica.global.lept.pixSauvolaBinarizeTiled;
import static org.bytedeco.leptonica.global.lept.pixScale;
import static org.bytedeco.leptonica.global.lept.pixThresholdToBinary;
import static org.bytedeco.leptonica.global.lept.pixWriteMemJpeg;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.ddak.yongha.vo.ReceiptData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOcrService {

    // ===== 정책/튜닝 =====
    private static final int   TARGET_MAX_BYTES       = 200 * 1024; // 200KB
    private static final int   MIN_WIDTH              = 300;
    private static final int   MIN_HEIGHT             = 300;
    private static final int   MAX_LONG_SIDE_PX       = 1800;       // 2~3K 원본 방어
    private static final int   DESKEW_TIME_LIMIT_MS   = 200;
    private static final int   FALLBACK_THRESH        = 180;
    private static final String LANGS                 = "kor+eng";

    private volatile String tessDataPath; // tessdata 디렉토리 절대경로

    /** classpath의 tessdata를 임시폴더로 복사하고, 그 "tessdata 폴더" 절대경로를 반환 */
    private synchronized String ensureTessdataPath() {
        if (tessDataPath != null) return tessDataPath;
        try {
            File tmpRoot = Files.createTempDirectory("tess-").toFile();
            File tessDir = new File(tmpRoot, "tessdata");
            if (!tessDir.mkdirs() && !tessDir.isDirectory()) {
                throw new IllegalStateException("tessdata 디렉토리 생성 실패: " + tessDir);
            }
            copyFromClasspath("tessdata/eng.traineddata", new File(tessDir, "eng.traineddata"));
            copyFromClasspath("tessdata/kor.traineddata", new File(tessDir, "kor.traineddata"));
            this.tessDataPath = tessDir.getAbsolutePath(); // tessdata 폴더 자체
            log.info("[OCR] tessdata={}", this.tessDataPath);
            return this.tessDataPath;
        } catch (Exception e) {
            throw new RuntimeException("tessdata 준비 실패: " + e.getMessage(), e);
        }
    }

    private static void copyFromClasspath(String classpath, File dest) throws Exception {
        var res = new ClassPathResource(classpath);
        if (!res.exists()) throw new IllegalStateException("리소스 없음: " + classpath);
        try (var in = res.getInputStream()) {
            Files.copy(in, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public ReceiptData ocr(byte[] imageBytes) {
        String datapath = ensureTessdataPath();

        PIX src = null, gray = null, graySized = null, ocrGray = null;
        PIX binForSkew = null;
        BytePointer mem = null;
        TessBaseAPI api = null;

        long T0 = System.currentTimeMillis();
        try {
            // 0) 메모리에서 로드
            mem = new BytePointer(imageBytes);
            src = pixReadMem(mem, imageBytes.length);
            if (src == null || src.isNull())
                throw new IllegalArgumentException("이미지 디코딩 실패 (JPG/PNG 확인)");
            int w0 = src.w(), h0 = src.h();
            log.info("[OCR] load ok ({}x{}, {} KB)", w0, h0, imageBytes.length / 1024);

            // 1) 8bpp 그레이
            gray = pixConvertTo8(src, 0);

            // 2) 크기 정규화: 초소형 업스케일 & 과대 다운스케일 (항상 8bpp에서)
            float up = 1.0f;
            if (w0 < MIN_WIDTH || h0 < MIN_HEIGHT) {
                float sw = (float) MIN_WIDTH / Math.max(1, w0);
                float sh = (float) MIN_HEIGHT / Math.max(1, h0);
                up = Math.max(sw, sh);
            }
            float down = 1.0f;
            int longSide0 = Math.max(w0, h0);
            if (longSide0 > MAX_LONG_SIDE_PX) {
                down = (float) MAX_LONG_SIDE_PX / longSide0;
            }
            float s0 = up * down;
            graySized = (Math.abs(s0 - 1.0f) > 1e-3) ? pixScale(gray, s0, s0) : gray;
            if (graySized != gray) pixDestroy(gray); // 교체

            // 3) 크기/용량 정책
            if (imageBytes.length <= TARGET_MAX_BYTES) {
                // 작은 입력: 재인코딩 X, 정보 보존 위해 업스케일
                ocrGray = pixClone(graySized);
                int ls = Math.max(ocrGray.w(), ocrGray.h());
                if (ls < 1200) {
                    PIX up2 = pixScale(ocrGray, 2.0f, 2.0f);
                    pixDestroy(ocrGray);
                    ocrGray = up2;
                    log.info("[OCR] small input → upscale x2 ({}x{})", ocrGray.w(), ocrGray.h());
                }
                int maxSide = Math.max(ocrGray.w(), ocrGray.h());
                if (maxSide > MAX_LONG_SIDE_PX) {
                    float s = (float) MAX_LONG_SIDE_PX / maxSide;
                    PIX down2 = pixScale(ocrGray, s, s);
                    pixDestroy(ocrGray);
                    ocrGray = down2;
                    log.info("[OCR] clamp to {}px (scale={})", MAX_LONG_SIDE_PX, s);
                }
            } else {
                // 큰 입력: 200KB 이하로 압축 재인코딩
                ocrGray = compressToUnder(graySized, TARGET_MAX_BYTES);
                if (ocrGray == null || ocrGray.isNull())
                    throw new IllegalStateException("압축/재로딩 실패");
                log.info("[OCR] after compress: {}x{} (~<=200KB)", ocrGray.w(), ocrGray.h());
            }

            // 4) 이진화(동적 타일) + Sauvola 폴백 → 스큐 추정
            int longSide = Math.max(ocrGray.w(), ocrGray.h());
            int tile = (longSide <= 800) ? 8 : (longSide <= 1200 ? 12 : (longSide <= 1800 ? 16 : 24));

            PointerPointer th1 = new PointerPointer(1), out1 = new PointerPointer(1);
            int rv1 = pixOtsuAdaptiveThreshold(ocrGray, tile, tile, 0, 0, 0.1f, th1, out1);
            PIX thPix = (rv1 == 0) ? new PIX(th1.get(PIX.class, 0)) : null;
            binForSkew = (rv1 == 0) ? new PIX(out1.get(PIX.class, 0)) : null;
            if (thPix != null && !thPix.isNull()) pixDestroy(thPix);

            if (binForSkew == null || binForSkew.isNull()) {
                PointerPointer thS = new PointerPointer(1), outS = new PointerPointer(1);
                int wh = (longSide <= 800) ? 15 : 25; // 작은 해상도에 유리
                int ok = pixSauvolaBinarizeTiled(ocrGray, wh, 0.35f, 1, 1, thS, outS);
                PIX thSpx = (ok == 0) ? new PIX(thS.get(PIX.class, 0)) : null;
                binForSkew = (ok == 0) ? new PIX(outS.get(PIX.class, 0)) : null;
                if (thSpx != null && !thSpx.isNull()) pixDestroy(thSpx);
                log.info("[OCR] used Sauvola (wh={}, factor=0.35)", wh);
            }
            if (binForSkew == null || binForSkew.isNull()) {
                binForSkew = pixThresholdToBinary(ocrGray, FALLBACK_THRESH);
                log.info("[OCR] used simple threshold {}", FALLBACK_THRESH);
            }

            long ts = System.currentTimeMillis();
            float[] angle = new float[1], conf = new float[1];
            pixFindSkew(binForSkew, angle, conf);
            long skewMs = System.currentTimeMillis() - ts;
            boolean doDeskew = skewMs <= DESKEW_TIME_LIMIT_MS && conf[0] > 2.0f && Math.abs(angle[0]) > 0.5f;
            if (doDeskew) {
                float rad = (float)(-angle[0] * Math.PI / 180.0);
                PIX rotated = pixRotateAMGray(ocrGray, rad, (byte)128);
                if (rotated != null && !rotated.isNull()) {
                    pixDestroy(ocrGray);
                    ocrGray = rotated;
                }
                log.info("[OCR] deskew {} ms (angle={}, conf={})", skewMs, angle[0], conf[0]);
            } else {
                log.info("[OCR] deskew skipped {} ms (angle={}, conf={})", skewMs, angle[0], conf[0]);
            }

            // 5) Tesseract 초기화 (1회, PSM=6 고정)
            api = new TessBaseAPI();
            int init = api.Init(datapath, LANGS);
            if (init != 0) throw new RuntimeException("Could not initialize tesseract.");
            api.SetVariable("debug_file", "NUL"); // 윈도우: 경고 로그 억제
            api.SetVariable("classify_bln_numeric_mode", "1");
            api.SetVariable("preserve_interword_spaces", "1");

            api.SetPageSegMode(6);
            api.SetImage(ocrGray);
            String text1 = safeText(api);

            api.Clear();
            api.SetPageSegMode(6);
            api.SetVariable("tessedit_char_whitelist", "0123456789, .₩\\원");
            api.SetImage(ocrGray);
            String textAmount = safeText(api);

            api.Clear();
            api.SetPageSegMode(6);
            api.SetVariable("tessedit_char_whitelist", "0123456789:-./년월일 시분");
            api.SetImage(ocrGray);
            String textDate = safeText(api);

            String merged = (text1 + "\n" + textAmount + "\n" + textDate);
            BigDecimal amount = ReceiptParser.pickAmount(merged);
            LocalDateTime paidAt = ReceiptParser.pickDateTime(merged);

            log.info("[OCR] total {} ms", System.currentTimeMillis() - T0);

            return ReceiptData.builder()
                    .totalAmount(amount)
                    .paidAt(paidAt)
                    .rawText(merged)
                    .build();

        } catch (Exception e) {
            log.error("[OCR] 실패: {}", e.getMessage(), e);
            throw new RuntimeException("OCR 실패", e);
        } finally {
            if (api != null) api.End();
            if (binForSkew != null) pixDestroy(binForSkew);
            if (ocrGray != null) pixDestroy(ocrGray);
            if (graySized != null && graySized != gray) pixDestroy(graySized);
            if (gray != null) pixDestroy(gray);
            if (src != null) pixDestroy(src);
            if (mem != null) mem.deallocate();
        }
    }

    /** 그레이(8bpp) PIX를 200KB 이하로 JPEG 재인코딩하여 다시 PIX(8bpp)로 로드 */
    private static PIX compressToUnder(PIX srcGray8, int targetBytes) {
        PIX working = srcGray8;
        float scale = 1.0f;
        int quality = 85;

        for (int iter = 0; iter < 12; iter++) {
            BytePointer out = new BytePointer((Pointer) null);
            SizeTPointer sz = new SizeTPointer(1);
            int rc = pixWriteMemJpeg(out, sz, working, quality, 0);
            if (rc != 0) {
                out.deallocate(); sz.deallocate();
                scale *= 0.85f;
                PIX scaled = pixScale(working, scale, scale);
                if (working != srcGray8) pixDestroy(working);
                working = scaled;
                quality = 85;
                continue;
            }
            long bytes = sz.get();
            if (bytes <= targetBytes) {
                PIX small = pixReadMem(out, (int) bytes);
                // 안전 위해 8bpp로 보장
                PIX smallGray = pixConvertTo8(small, 0);
                pixDestroy(small);
                out.deallocate(); sz.deallocate();
                if (working != srcGray8) pixDestroy(working);
                return smallGray;
            }
            out.deallocate(); sz.deallocate();
            if (quality > 45) {
                quality -= 10;
            } else {
                scale *= 0.85f;
                PIX scaled = pixScale(working, scale, scale);
                if (working != srcGray8) pixDestroy(working);
                working = scaled;
                quality = 75;
            }
            if (working.w() < 600 && working.h() < 400 && quality <= 45) {
                BytePointer out2 = new BytePointer((Pointer) null);
                SizeTPointer sz2 = new SizeTPointer(1);
                pixWriteMemJpeg(out2, sz2, working, 40, 0);
                PIX small = pixReadMem(out2, (int) sz2.get());
                PIX smallGray = pixConvertTo8(small, 0);
                pixDestroy(small);
                out2.deallocate(); sz2.deallocate();
                if (working != srcGray8) pixDestroy(working);
                return smallGray;
            }
        }
        if (working != srcGray8) {
            PIX ret = working; // 이미 8bpp
            return ret;
        }
        return srcGray8;
    }

    private static String safeText(TessBaseAPI api) {
        var bp = api.GetUTF8Text();
        if (bp == null) return "";
        try {
            return bp.getString().trim();
        } finally {
            bp.deallocate();
        }
    }
}
