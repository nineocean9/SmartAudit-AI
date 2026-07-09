package com.ruoyi.system.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档切块工具
 *
 * 策略：
 * 1. 优先按双换行(\n\n)切分段落
 * 2. 单段超过 maxChars 则按句号/分号二次切分
 * 3. 相邻块之间保留 overlapChars 字符的重叠窗口
 *
 * @author ruoyi
 */
public class DocChunker
{
    /** 默认每块最大字符数 */
    public static final int DEFAULT_MAX_CHARS = 512;

    /** 默认重叠字符数 */
    public static final int DEFAULT_OVERLAP = 64;

    /** 句子分隔符正则 */
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("([。；;！!？?\\n])");

    /**
     * 使用默认参数切块
     */
    public static List<String> chunk(String text)
    {
        return chunk(text, DEFAULT_MAX_CHARS, DEFAULT_OVERLAP);
    }

    /**
     * @param text         原始文本
     * @param maxChars     每块最大字符数
     * @param overlapChars 相邻块重叠字符数
     * @return 切块列表
     */
    public static List<String> chunk(String text, int maxChars, int overlapChars)
    {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank())
        {
            return chunks;
        }

        // Step 1: 按双换行拆分为段落
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs)
        {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            // 如果当前块 + 新段落 超出限制，先保存当前块
            if (currentChunk.length() > 0
                && currentChunk.length() + trimmed.length() + 1 > maxChars)
            {
                chunks.add(currentChunk.toString().trim());
                // 保留重叠部分
                if (overlapChars > 0 && currentChunk.length() > overlapChars)
                {
                    currentChunk = new StringBuilder(
                            currentChunk.substring(currentChunk.length() - overlapChars));
                }
                else
                {
                    currentChunk = new StringBuilder();
                }
            }

            // 单段落超过 maxChars，按句子二次切分
            if (trimmed.length() > maxChars)
            {
                List<String> subChunks = splitLongParagraph(trimmed, maxChars, overlapChars);
                for (int i = 0; i < subChunks.size(); i++)
                {
                    String sub = subChunks.get(i);
                    if (i == 0 && currentChunk.length() > 0)
                    {
                        // 第一段追加到当前块
                        if (currentChunk.length() + sub.length() + 1 <= maxChars + overlapChars)
                        {
                            currentChunk.append("\n").append(sub);
                            continue;
                        }
                        else
                        {
                            chunks.add(currentChunk.toString().trim());
                            currentChunk = new StringBuilder(sub);
                            continue;
                        }
                    }
                    if (currentChunk.length() > 0)
                    {
                        chunks.add(currentChunk.toString().trim());
                    }
                    currentChunk = new StringBuilder(sub);
                }
            }
            else
            {
                if (currentChunk.length() > 0)
                {
                    currentChunk.append("\n");
                }
                currentChunk.append(trimmed);
            }
        }

        // 最后一块
        if (currentChunk.length() > 0)
        {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 切分超长段落（按句子边界）
     */
    private static List<String> splitLongParagraph(String text, int maxChars, int overlapChars)
    {
        List<String> result = new ArrayList<>();

        // 先尝试按句子分隔符切分
        List<String> sentences = new ArrayList<>();
        Matcher m = SENTENCE_SPLIT.matcher(text);
        int lastEnd = 0;
        while (m.find())
        {
            int end = m.end();
            sentences.add(text.substring(lastEnd, end).trim());
            lastEnd = end;
        }
        if (lastEnd < text.length())
        {
            sentences.add(text.substring(lastEnd).trim());
        }

        // 如果切分后仍然全为空或无效，回退到硬切分
        if (sentences.isEmpty() || (sentences.size() == 1 && sentences.get(0).length() > maxChars))
        {
            return hardSplit(text, maxChars, overlapChars);
        }

        // 组装句子为不超过 maxChars 的块
        StringBuilder chunk = new StringBuilder();
        for (String sentence : sentences)
        {
            if (sentence.isEmpty()) continue;

            if (chunk.length() + sentence.length() > maxChars && chunk.length() > 0)
            {
                result.add(chunk.toString().trim());
                // 重叠
                if (overlapChars > 0 && chunk.length() > overlapChars)
                {
                    String saved = chunk.toString();
                    chunk = new StringBuilder(saved.substring(Math.max(0, saved.length() - overlapChars)));
                }
                else
                {
                    chunk = new StringBuilder();
                }
            }
            chunk.append(sentence);
        }
        if (chunk.length() > 0)
        {
            result.add(chunk.toString().trim());
        }

        return result;
    }

    /**
     * 硬切分：直接按字符数截断（用于没有合适句子边界的情况）
     */
    private static List<String> hardSplit(String text, int maxChars, int overlapChars)
    {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length())
        {
            int end = Math.min(start + maxChars, text.length());
            result.add(text.substring(start, end).trim());
            start = end - overlapChars;
            if (start >= text.length()) break;
        }
        return result;
    }

    /**
     * 估算 Token 数
     * 中英文混合：中文约1.5字符/token，英文约4字符/token
     */
    public static int estimateTokens(String text)
    {
        if (text == null || text.isEmpty()) return 0;

        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray())
        {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
            {
                chineseChars++;
            }
            else if (!Character.isWhitespace(c))
            {
                otherChars++;
            }
        }
        return (int) Math.ceil(chineseChars / 1.5 + otherChars / 4.0);
    }
}
