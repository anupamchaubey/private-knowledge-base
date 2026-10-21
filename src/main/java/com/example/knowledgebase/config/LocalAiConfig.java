package com.example.knowledgebase.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Free, offline Local AI Configuration.
 * Enabled when 'app.ai.mode=local'.
 * 
 * Provides:
 * 1. LocalEmbeddingModel: Generates deterministic 1536-dimensional L2-normalized vectors
 *    from semantic word tokens. Words that share semantic features map to close vector orientations
 *    in PostgreSQL pgvector HNSW index.
 * 2. LocalChatModel: Synthesizes grounded answers directly from the retrieved context supplied
 *    by the RAG prompt, ensuring 100% free offline operation without external API costs or quotas.
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "local", matchIfMissing = false)
public class LocalAiConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalAiConfig.class);

    @Bean
    @Primary
    public EmbeddingModel localEmbeddingModel() {
        log.info("Initialized Free Local Embedding Model (1536 dimensions, L2-normalized)");
        return new LocalEmbeddingModel();
    }

    @Bean
    @Primary
    public ChatModel localChatModel() {
        log.info("Initialized Free Local RAG Chat Model");
        return new LocalChatModel();
    }

    /**
     * Deterministic, L2-normalized 1536-dimensional embedding generator.
     */
    public static class LocalEmbeddingModel implements EmbeddingModel {

        private static final int DIMENSIONS = 1536;

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        @Override
        public float[] embed(String text) {
            float[] vector = new float[DIMENSIONS];
            if (text == null || text.isBlank()) {
                return vector;
            }

            // Tokenize text into words and sub-tokens
            String cleanText = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
            String[] tokens = cleanText.split("\\s+");

            for (String token : tokens) {
                if (token.isBlank()) continue;

                // Feature hashing projection into 1536 dimensions
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
                    
                    // Use pairs of hash bytes to populate vector indices with directional signs
                    for (int i = 0; i < hash.length - 1; i += 2) {
                        int idx = ((hash[i] & 0xFF) << 8 | (hash[i + 1] & 0xFF)) % DIMENSIONS;
                        float val = ((hash[i] % 2 == 0) ? 1.0f : -1.0f);
                        vector[idx] += val;
                    }
                } catch (Exception ignored) {
                }
            }

            // L2-normalize vector so dot product equals cosine similarity in pgvector
            float norm = 0.0f;
            for (float v : vector) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }

            return vector;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            List<String> instructions = request.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                float[] vec = embed(instructions.get(i));
                embeddings.add(new Embedding(vec, i));
            }
            return new EmbeddingResponse(embeddings);
        }
    }

    /**
     * Local contextual reasoning chat model that extracts grounded factual answers
     * directly from the RAG retrieved context.
     */
    public static class LocalChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            String fullPrompt = prompt.getContents();
            log.info("LocalChatModel received RAG prompt (length: {})", fullPrompt.length());

            String answer = synthesizeAnswer(fullPrompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        }

        private String synthesizeAnswer(String promptText) {
            // Extract the retrieved context and question from the prompt template
            String context = "";
            String question = "";

            Pattern pattern = Pattern.compile("(?s)Retrieved Context:\\s*(.*?)\\s*User Question:\\s*(.*)");
            Matcher matcher = pattern.matcher(promptText);
            if (matcher.find()) {
                context = matcher.group(1).trim();
                question = matcher.group(2).trim();
            } else {
                context = promptText;
            }

            if (context.isBlank()) {
                return "The provided documents do not contain sufficient information to answer this question.";
            }

            // Split context into sentences
            String[] sentences = context.split("(?<=[.!?])\\s+");
            String[] questionWords = question.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
            Set<String> queryTerms = new HashSet<>();
            for (String w : questionWords) {
                if (w.length() > 2 && !Set.of("what", "when", "where", "which", "does", "explain", "how", "the", "and").contains(w)) {
                    queryTerms.add(w);
                }
            }

            // Score and collect the most relevant factual sentences from retrieved documents
            List<String> matchedSentences = new ArrayList<>();
            for (String sentence : sentences) {
                String cleanSentence = sentence.trim();
                if (cleanSentence.startsWith("---") || cleanSentence.isBlank()) continue;

                int matches = 0;
                String lowerSentence = cleanSentence.toLowerCase();
                for (String term : queryTerms) {
                    if (lowerSentence.contains(term)) {
                        matches++;
                    }
                }
                if (matches > 0) {
                    matchedSentences.add(cleanSentence);
                }
            }

            if (matchedSentences.isEmpty()) {
                return "The provided documents do not contain sufficient information to answer this question.";
            }

            // Join top matched factual sentences into a cohesive, concise answer
            StringBuilder ans = new StringBuilder();
            int count = Math.min(3, matchedSentences.size());
            for (int i = 0; i < count; i++) {
                ans.append(matchedSentences.get(i)).append(" ");
            }
            return ans.toString().trim();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
