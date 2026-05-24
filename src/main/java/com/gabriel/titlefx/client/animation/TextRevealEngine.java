package com.gabriel.titlefx.client.animation;

import com.gabriel.titlefx.client.render.AnimatedTextInstance;
import com.gabriel.titlefx.client.render.RenderableGlyph;
import com.gabriel.titlefx.common.animation.RevealType;
import com.gabriel.titlefx.common.model.RevealPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TextRevealEngine {

    public static void update(AnimatedTextInstance instance, long elapsedMs) {
        RevealPayload reveal = instance.getLayer().reveal();
        RevealType type = reveal.type();
        int duration = reveal.durationMs();
        List<RenderableGlyph> glyphs = instance.getGlyphs();

        if (duration <= 0 || type == RevealType.NONE) {
            // Entire text is fully visible immediately
            for (RenderableGlyph glyph : glyphs) {
                glyph.visible = true;
                glyph.currentChar = glyph.targetChar;
            }
            return;
        }

        float t = Math.max(0.0f, Math.min(1.0f, (float) elapsedMs / duration));

        switch (type) {
            case TYPEWRITER:
                updateTypewriter(instance, t);
                break;
            case WORD_BY_WORD:
                updateWordByWord(instance, t);
                break;
            case LINE_BY_LINE:
                updateLineByLine(instance, t);
                break;
            case OBFUSCATED_DECODE:
            case GLYPH_SCRAMBLE:
                updateDecodeOrScramble(instance, elapsedMs, t);
                break;
            case NONE:
            default:
                for (RenderableGlyph glyph : glyphs) {
                    glyph.visible = true;
                    glyph.currentChar = glyph.targetChar;
                }
                break;
        }
    }

    private static void updateTypewriter(AnimatedTextInstance instance, float t) {
        List<RenderableGlyph> glyphs = instance.getGlyphs();
        List<Integer> revealOrder = instance.getRevealOrder();
        int visibleCount = (int) (t * glyphs.size());

        for (int i = 0; i < glyphs.size(); i++) {
            int index = revealOrder.get(i);
            RenderableGlyph glyph = glyphs.get(index);
            glyph.visible = (i < visibleCount || t >= 1.0f);
        }
    }

    private static void updateWordByWord(AnimatedTextInstance instance, float t) {
        String text = instance.getText();
        List<RenderableGlyph> glyphs = instance.getGlyphs();
        
        // Find word boundary indices
        List<WordRange> words = findWordRanges(text);
        if (words.isEmpty()) {
            for (RenderableGlyph glyph : glyphs) glyph.visible = true;
            return;
        }

        int visibleWordCount = (int) (t * words.size());
        if (t >= 1.0f) visibleWordCount = words.size();

        for (int i = 0; i < glyphs.size(); i++) {
            glyphs.get(i).visible = false;
        }

        // Make characters of visible words visible
        for (int w = 0; w < visibleWordCount; w++) {
            WordRange range = words.get(w);
            for (int i = range.start; i < range.end; i++) {
                glyphs.get(i).visible = true;
            }
        }

        // Make spaces visible between visible words
        for (int i = 0; i < glyphs.size(); i++) {
            if (text.charAt(i) == ' ') {
                // If there's a visible word before and after, or if we show at least one word
                // Let's make space visible if the preceding word is visible
                int precedingWordIdx = findPrecedingWord(words, i);
                if (precedingWordIdx != -1 && precedingWordIdx < visibleWordCount) {
                    glyphs.get(i).visible = true;
                }
            }
        }
    }

    private static void updateLineByLine(AnimatedTextInstance instance, float t) {
        String text = instance.getText();
        List<RenderableGlyph> glyphs = instance.getGlyphs();
        
        // Find line ranges based on '\n'
        List<LineRange> lines = findLineRanges(text);
        if (lines.isEmpty()) {
            for (RenderableGlyph glyph : glyphs) glyph.visible = true;
            return;
        }

        int visibleLineCount = (int) (t * lines.size());
        if (t >= 1.0f) visibleLineCount = lines.size();

        for (int i = 0; i < glyphs.size(); i++) {
            glyphs.get(i).visible = false;
        }

        for (int l = 0; l < visibleLineCount; l++) {
            LineRange range = lines.get(l);
            for (int i = range.start; i < range.end; i++) {
                glyphs.get(i).visible = true;
            }
        }
    }

    private static void updateDecodeOrScramble(AnimatedTextInstance instance, long elapsedMs, float t) {
        List<RenderableGlyph> glyphs = instance.getGlyphs();
        List<Integer> revealOrder = instance.getRevealOrder();
        RevealPayload reveal = instance.getLayer().reveal();
        
        String charset = reveal.charset();
        if (charset == null || charset.isEmpty()) {
            charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        }

        int lockedCount = (int) (t * glyphs.size());
        if (t >= 1.0f) lockedCount = glyphs.size();

        int flickerSpeed = Math.max(1, reveal.flickerSpeed());
        long period = Math.max(10, 1000 / (flickerSpeed * 5)); // rate of character changing
        
        Random rand = new Random();

        for (int i = 0; i < glyphs.size(); i++) {
            int charIndex = revealOrder.get(i);
            RenderableGlyph glyph = glyphs.get(charIndex);
            
            if (i < lockedCount) {
                glyph.visible = true;
                glyph.currentChar = glyph.targetChar;
            } else {
                glyph.visible = true;
                if (glyph.targetChar == ' ' || glyph.targetChar == '\n') {
                    glyph.currentChar = glyph.targetChar;
                } else {
                    long seed = (elapsedMs / period) + charIndex;
                    rand.setSeed(seed);
                    char scrambleChar = charset.charAt(rand.nextInt(charset.length()));
                    
                    if (reveal.preserveCase()) {
                        if (Character.isUpperCase(glyph.targetChar)) {
                            scrambleChar = Character.toUpperCase(scrambleChar);
                        } else {
                            scrambleChar = Character.toLowerCase(scrambleChar);
                        }
                    }
                    glyph.currentChar = scrambleChar;
                }
            }
        }
    }

    // Helpers
    private static class WordRange {
        int start;
        int end;
        WordRange(int s, int e) { start = s; end = e; }
    }

    private static List<WordRange> findWordRanges(String text) {
        List<WordRange> ranges = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\n') {
                if (start == -1) start = i;
            } else {
                if (start != -1) {
                    ranges.add(new WordRange(start, i));
                    start = -1;
                }
            }
        }
        if (start != -1) {
            ranges.add(new WordRange(start, text.length()));
        }
        return ranges;
    }

    private static int findPrecedingWord(List<WordRange> words, int charIdx) {
        for (int w = 0; w < words.size(); w++) {
            if (words.get(w).end <= charIdx) {
                if (w == words.size() - 1 || words.get(w + 1).start > charIdx) {
                    return w;
                }
            }
        }
        return -1;
    }

    private static class LineRange {
        int start;
        int end;
        LineRange(int s, int e) { start = s; end = e; }
    }

    private static List<LineRange> findLineRanges(String text) {
        List<LineRange> ranges = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                ranges.add(new LineRange(start, i));
                start = i + 1;
            }
        }
        ranges.add(new LineRange(start, text.length()));
        return ranges;
    }
}
