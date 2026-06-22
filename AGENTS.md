# Backend Project Instructions for Codex

## My identity

I am a junior undergraduate student majoring in Computer Science and Technology.

I am currently learning Java backend development, especially Spring Boot, REST APIs, database operations, and layered backend architecture.

This backend project is not only a coding project, but also a learning project. Please do not treat me as an experienced professional backend developer.

The priority of this backend project is:

1. Learn backend development step by step.
2. Build a backend project that can be used for internship and job application demonstration.
3. Gradually improve code quality.
4. Add more advanced backend features only after the basic features are clear and working.

## My technical background

I have basic knowledge of Java, object-oriented programming, collections, exceptions, generics, lambda expressions, streams, MySQL, HTTP, REST APIs, and simple Spring Boot CRUD development.

I can understand basic Java code and can write simple Spring Boot CRUD features by following a clear implementation idea.

I have learned the basic layered structure of a backend project, including:

* Controller
* Service
* Repository
* Entity / Model
* DTO
* JDBC Template
* basic validation
* pagination
* simple REST API design

I have also learned some basic network programming concepts, such as BIO, NIO, Socket, Selector, Channel, and ByteBuffer.

However, I am still a beginner in enterprise-level backend development. I am not yet familiar with complex system design, advanced design patterns, permission systems, logging frameworks, distributed systems, complex exception architecture, or production-level engineering practices.

When new backend concepts appear, please explain them clearly. Do not assume that I already understand enterprise-level conventions.

## My backend learning goals

My main goal is to become a backend development engineer.

I want this backend project to become useful for internships, job applications, and technical interviews.

My current main direction is Java backend development with Spring Boot.

The backend learning priorities are:

* Spring Boot project structure
* REST API design
* Controller / Service / Repository responsibilities
* JDBC Template and database operations
* DTO usage
* request parameters
* validation
* pagination
* basic error handling
* database table relationships
* frontend-backend API contract design
* code readability and maintainability

In the future, I may want to add some AI-related backend application features, such as:

* AI content summarization
* RAG-based knowledge retrieval
* AI-assisted article processing
* simple agent-like workflows

But these AI features are not the current priority. Do not let future AI plans interfere with the current backend learning pace.

## Preferred backend development style

Please follow an MVP-first learning approach.

When implementing a backend feature, first help me complete the simplest correct version that satisfies the current requirement.

Do not introduce advanced backend architecture too early.

Do not add extra layers, frameworks, design patterns, or utility classes unless the current requirement clearly needs them.

I prefer learning by gradually encountering real problems and then solving them, instead of building a complete enterprise-level backend framework from the beginning.

Before adding any new abstraction, framework, common utility, global mechanism, or design pattern, first ask:

"Is this necessary for the current requirement, or is it premature engineering?"

If it is not necessary now, do not implement it. Mention it as a TODO at the end instead.

When explaining or implementing backend code, follow a request-driven flow that is easy for a beginner to understand: start from the frontend request, show how it is received by the Controller, then explain why the Controller calls the Service for business logic, and why the Service calls the Repository for database access. Write code in this order when possible, instead of introducing layers or abstractions before they are needed by the current requirement.


## Explanation style

When helping me develop backend features, please use this style:

1. Briefly explain what backend problem we are solving.
2. Explain the minimal implementation idea.
3. List the backend files that need to be changed.
4. Provide the code changes.
5. Explain only the new or important concepts.
6. Avoid repeatedly explaining concepts that have already appeared many times.
7. At the end, list possible future improvements as TODOs, but do not implement them unless I ask.

Please avoid giving me a large amount of code without explanation.

Please avoid using vague phrases such as "best practice", "enterprise standard", or "production-ready" without explaining why they are needed.

Prefer clear, direct, and beginner-friendly backend code over clever or overly abstract code.

## Things to avoid

Please avoid the following unless I explicitly ask for them or the current requirement clearly needs them:

* complex common layers
* global exception handling systems
* advanced error code frameworks
* permission and authentication systems
* logging frameworks
* complex DTO converters
* excessive abstraction
* premature design patterns
* large-scale refactoring
* replacing simple code with clever but hard-to-understand code
* introducing many new files just for a small backend feature
* designing for problems that have not appeared yet
* changing frontend code
* making frontend UI design decisions
* adding frontend frameworks or frontend dependencies

If these things may be useful later, mention them as TODOs instead of implementing them immediately.

## Backend project boundaries

This Codex context is for backend development only.

Do not modify frontend files unless I explicitly ask.

Do not make UI design decisions in this backend context.

When frontend-backend interaction is involved, only focus on the backend API contract, including:

* request method
* request path
* path variables
* query parameters
* request body format
* response body format
* status code
* validation rules
* error behavior

If frontend changes are required, describe the required API behavior clearly, but do not implement frontend code in this backend project.

## Spring Boot backend preferences

For each new backend feature, keep the implementation small and understandable first.

Prefer this order:

1. Make the feature work correctly.
2. Make the code clear and readable.
3. Add necessary validation or error handling.
4. Only then consider further optimization.

When using Controller, Service, and Repository layers, please explain why each layer is involved if the reason is not obvious.

When using DTOs, please explain whether the DTO is for request data, response data, or pagination data.

When writing SQL or JDBC Template code, please explain:

* what the SQL does
* what each parameter means
* how query results are mapped to Java objects
* why the return value is needed

## Important rule

The top priority of this backend project is learning through understandable implementation.

Do not optimize for enterprise-grade completeness too early.

Do not silently make the backend project more complex than the current requirement needs.

When in doubt, choose the simpler backend solution first and explain what can be improved later.
