# College Fresher Interview Guide & Resume Cheatsheet
## Private Knowledge Base API (Spring Boot + Spring AI + pgvector)

---

## 1. How to Write This on Your Resume

Put this under your **Projects** section. Choose the format that best fits your resume layout.

### Project Title & Tech Stack:
**Private Knowledge Base API — Secure RAG Backend** | *Java 21, Spring Boot 3, Spring AI, PostgreSQL (pgvector), Spring Security (JWT), Docker*

### Bullet Points (Copy & Paste Ready):
- **Built a secure multi-tenant RAG (Retrieval-Augmented Generation) backend API** using Spring Boot 3 and Spring AI, enabling users to upload private PDFs and ask contextual questions.
- **Implemented automated document ingestion pipeline** utilizing Apache PDFBox for text extraction and `TokenTextSplitter` to partition documents into 500-token chunks with 50-token overlap.
- **Integrated PostgreSQL pgvector with HNSW cosine distance indexing** to store and perform semantic similarity searches on 1536-dimensional vector embeddings with metadata filtering.
- **Engineered multi-tenant data isolation and Spring Security JWT authentication**, guaranteeing zero cross-user vector leakage through strict database-level filtering.
- **Designed anti-hallucination guardrails** including similarity threshold gating (≥0.35) and deterministic source citations (document name, page number, and similarity score).
- **Achieved 100% test coverage for core business logic** with 23 JUnit 5 and Mockito tests; documented REST endpoints via Springdoc OpenAPI (Swagger UI).

---

## 2. Boundaries: What Is Your "Safe Zone" vs "Danger Zone"?

As a college fresher, you are **not** expected to know AI mathematics or complex distributed clusters. Stick to this boundary:

| Topic | 🟢 SAFE ZONE (Talk confidently!) | 🔴 DANGER ZONE (Do NOT volunteer this!) |
| :--- | :--- | :--- |
| **Your Role** | Backend Developer who integrated AI APIs and handled data storage & security | "I built an AI model" or "I trained a machine learning model" |
| **Embeddings** | "An embedding is a list of 1536 numbers representing the semantic meaning of text." | Matrix mathematics, backpropagation, transformer layer equations |
| **Vector DB** | "We used PostgreSQL with pgvector extension to store vectors and calculate cosine distance." | Writing custom C++ clustering algorithms or CUDA GPU kernels |
| **Architecture** | "Clean layered monolithic REST API with Spring Boot (Controller, Service, Repository)." | Overcomplicated microservices, Kafka, Kubernetes, event-sourcing |
| **RAG** | "It's an open-book exam: find relevant PDF paragraphs first, then let the LLM answer from them." | Fine-tuning, RLHF (Reinforcement Learning with Human Feedback), LoRA |

### 🛡️ The "Magic Deflect Phrase" (If an interviewer asks deep AI math):
> *"In this project, my role was strictly backend engineering and systems integration. I treated the embedding generation and LLM inference as managed services through the Spring AI framework. My core focus was document parsing, multi-tenant database security, pgvector similarity search, and building resilient REST APIs."*

---

## 3. The 4 Interview Domains & What to Prepare

### Domain 1: Core Java & Spring Boot (40% of the interview)
Expect standard backend fresher questions:
1. **Why Constructor Injection instead of `@Autowired`?**
   - *Answer*: It makes dependencies immutable (`final`), prevents `NullPointerException` during bean initialization, and makes unit testing simple without needing Spring context reflection.
2. **Explain Controller vs Service vs Repository:**
   - *Answer*:
     - **Controller**: Handles HTTP requests, input validation (`@Valid`), and response status codes.
     - **Service**: Contains business logic, authorization rules, and workflows.
     - **Repository**: Handles database queries via Spring Data JPA.
3. **How do you handle exceptions globally?**
   - *Answer*: Using `@RestControllerAdvice` and `@ExceptionHandler`. It catches exceptions across the app and returns standardized JSON errors (`ErrorResponse`) so raw stack traces never leak to the client.

---

### Domain 2: Spring Security & Authentication (25% of the interview)
1. **What is a JWT and how does it work?**
   - *Answer*: JSON Web Token has three parts: **Header** (algorithm), **Payload** (claims like `userId` and `exp`), and **Signature** (cryptographic hash using a secret key). The client sends `Authorization: Bearer <token>` on each request.
2. **Why is session creation policy `STATELESS`?**
   - *Answer*: The server stores no session data in memory. Every request authenticates independently via JWT. This makes the application lightweight and easy to scale horizontally.
3. **Why BCrypt instead of MD5/SHA-256?**
   - *Answer*: SHA-256 is fast and vulnerable to GPU brute-force attacks. BCrypt is a slow, salted hashing function with an adjustable work factor that makes brute-forcing computationally prohibitive.

---

### Domain 3: PostgreSQL & Database Design (20% of the interview)
1. **What is pgvector?**
   - *Answer*: It is an open-source extension for PostgreSQL that adds a `vector` data type and distance operators (like cosine distance `<=>`). It allows us to store vector embeddings directly in PostgreSQL alongside regular relational tables.
2. **Why HNSW index?**
   - *Answer*: HNSW (Hierarchical Navigable Small World) is a graph-based indexing technique that enables fast approximate nearest-neighbor search without requiring a separate training or clustering step.
3. **How do you prevent User A from seeing User B's documents?**
   - *Answer*: At the database query level! We extract the trusted `userId` from the JWT token and pass it as a metadata filter (`FilterExpressionBuilder.eq("userId", userId)`) directly into the pgvector query.

---

### Domain 4: Practical RAG & Spring AI (15% of the interview)
1. **What is RAG in simple words?**
   - *Answer*: Retrieval-Augmented Generation. Instead of asking an AI model to answer purely from memory (which causes hallucinations), RAG searches our uploaded PDF chunks for relevant facts first, pastes them into the prompt, and tells the model to answer only using those facts.
2. **Why do we need chunking?**
   - *Answer*: A 50-page PDF is too large to embed into a single vector without losing specific details. Splitting into ~500-token chunks ensures each vector represents a focused concept.
3. **Why 50-token overlap between chunks?**
   - *Answer*: To prevent context from being cut in half if an important sentence falls right on the split boundary.
4. **How do you prevent hallucinations?**
   - *Answer*: Two safeguards:
     1. **Similarity Threshold**: If no retrieved chunks have a similarity score above `0.35`, the system immediately returns *"I do not have sufficient information in your uploaded documents."* without calling the LLM.
     2. **Strict System Prompt**: The prompt instructs the model to only use the provided text and not invent facts.

---

## 4. The 30-Second Elevator Pitch (Memorize This!)

> *"For my project, I built a **Private Knowledge Base API** using **Java 21, Spring Boot 3, and Spring AI**. It allows users to securely upload private PDF documents and ask questions about them.
>
> On the backend, the system validates the PDF, extracts and chunks the text, and stores 1536-dimensional vector embeddings in **PostgreSQL using the pgvector extension**. When a user asks a question, it runs a similarity search scoped strictly to that user's documents, sends the relevant paragraphs to an LLM, and returns a grounded answer with exact page citations and similarity scores.
>
> I implemented **Spring Security with JWT** for stateless authentication, ensured tenant data isolation, and wrote a full test suite with JUnit 5."*
