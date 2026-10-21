package com.example.knowledgebase;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.File;

public class PdfGeneratorTest {

    @Test
    void generateSamplePdf() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            // Page 1
            PDPage p1 = new PDPage();
            doc.addPage(p1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, p1)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.setLeading(20f);
                cs.newLineAtOffset(50, 720);
                cs.showText("Page 1: Spring Boot Core Architecture and Dependency Injection");
                cs.newLine();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.showText("Spring Boot simplifies enterprise application development through convention over configuration.");
                cs.newLine();
                cs.showText("Dependency Injection allows the IoC container to wire collaborators together at runtime.");
                cs.newLine();
                cs.showText("Constructor injection is preferred over field injection because it ensures immutability,");
                cs.newLine();
                cs.showText("guarantees complete object instantiation, and makes unit testing straightforward without reflection.");
                cs.endText();
            }

            // Page 2
            PDPage p2 = new PDPage();
            doc.addPage(p2);
            try (PDPageContentStream cs = new PDPageContentStream(doc, p2)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.setLeading(20f);
                cs.newLineAtOffset(50, 720);
                cs.showText("Page 2: Java 21 Virtual Threads and Concurrency");
                cs.newLine();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.showText("Project Loom introduced Virtual Threads in Java 21 as lightweight implementations of java.lang.Thread.");
                cs.newLine();
                cs.showText("Virtual threads are scheduled by the JVM onto carrier platform threads rather than operating system threads.");
                cs.newLine();
                cs.showText("When a virtual thread blocks on blocking I/O like database socket reads or REST calls,");
                cs.newLine();
                cs.showText("the JVM unmounts it from the carrier thread, allowing other virtual threads to execute.");
                cs.newLine();
                cs.showText("This dramatically increases throughput for high-concurrency I/O-bound web services.");
                cs.endText();
            }

            // Page 3
            PDPage p3 = new PDPage();
            doc.addPage(p3);
            try (PDPageContentStream cs = new PDPageContentStream(doc, p3)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.setLeading(20f);
                cs.newLineAtOffset(50, 720);
                cs.showText("Page 3: PostgreSQL Vector Indexing and Semantic Search");
                cs.newLine();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.showText("pgvector enables vector similarity search directly within PostgreSQL relational databases.");
                cs.newLine();
                cs.showText("The HNSW (Hierarchical Navigable Small World) index creates a multi-layered graph for approximate nearest neighbors.");
                cs.newLine();
                cs.showText("HNSW provides high recall and low search latency without needing an offline training step like IVFFlat.");
                cs.newLine();
                cs.showText("Cosine similarity measures the orientation between vectors rather than their magnitudes.");
                cs.endText();
            }

            doc.save(new File("sample_java_notes.pdf"));
        }
        System.out.println("Generated sample_java_notes.pdf successfully");
    }
}
