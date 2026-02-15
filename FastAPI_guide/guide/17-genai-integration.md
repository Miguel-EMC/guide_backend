# GenAI Integration with FastAPI

This chapter explains how to integrate AI/LLM capabilities into FastAPI apps, with examples for streaming, RAG, and best practices.

## Why FastAPI for GenAI

| Advantage | Why It Matters |
|----------|----------------|
| Async | Concurrency for multiple LLM calls |
| Streaming | Real-time responses |
| Validation | Robust input/output schemas |
| Performance | Low latency |
| OpenAPI | Automatic documentation |

## Environment Setup

```bash
# Base
pip install "fastapi[standard]"

# OpenAI
pip install openai

# Vector DBs (choose your stack)
pip install pinecone-client qdrant-client weaviate-client

# Local LLMs
pip install ollama
```

Typical environment variables:

```bash
OPENAI_API_KEY=your_openai_key
```

## OpenAI (Responses API)

The recommended API for new projects is **Responses**. It supports input as a string or a list of messages and returns output items plus a convenient `output_text` helper.

### Simple response

```python
from openai import OpenAI

client = OpenAI()

response = client.responses.create(
    model="gpt-5",
    input="Write a one-sentence bedtime story about a unicorn.",
)

print(response.output_text)
```

### FastAPI endpoint (message list)

```python
import asyncio
from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field
from openai import OpenAI

app = FastAPI()


class Message(BaseModel):
    role: str = Field(..., description="system, user, assistant")
    content: str


class ChatRequest(BaseModel):
    messages: list[Message]
    model: str = "gpt-5"
    temperature: float = Field(default=0.7, ge=0, le=2)


class ChatResponse(BaseModel):
    text: str
    model: str


def get_openai_client() -> OpenAI:
    return OpenAI()


@app.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    client: OpenAI = Depends(get_openai_client),
):
    try:
        response = await asyncio.to_thread(
            client.responses.create,
            model=request.model,
            input=[{"role": m.role, "content": m.content} for m in request.messages],
            temperature=request.temperature,
        )
        return ChatResponse(
            text=response.output_text,
            model=getattr(response, "model", request.model),
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))
```

## Streaming (SSE)

Streaming with the Responses API emits semantic events. Common ones to listen for are `response.created`, `response.output_text.delta`, `response.output_text.done`, `response.completed`, and `error`.

```python
import json
from fastapi import Depends
from fastapi.responses import StreamingResponse
from openai import OpenAI


class StreamingRequest(BaseModel):
    prompt: str
    model: str = "gpt-5"


def generate_stream(prompt: str, model: str, client: OpenAI):
    stream = client.responses.create(
        model=model,
        input=[{"role": "user", "content": prompt}],
        stream=True,
    )

    for event in stream:
        if event.type == "response.output_text.delta":
            yield f"data: {json.dumps({'content': event.delta})}\n\n"
        elif event.type == "response.completed":
            yield f"data: {json.dumps({'done': True})}\n\n"
        elif event.type == "error":
            yield f"data: {json.dumps({'error': 'stream_error'})}\n\n"


@app.post("/stream")
async def stream_chat(
    request: StreamingRequest,
    client: OpenAI = Depends(get_openai_client),
):
    return StreamingResponse(
        generate_stream(request.prompt, request.model, client),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache"},
    )
```

Note: streaming makes partial-content moderation harder, so apply policies or filters.

## RAG with Vector DBs (Conceptual Example)

```python
async def search_context(query: str) -> str:
    # 1) Retrieve relevant documents
    # 2) Build context
    return "relevant context"


@app.post("/rag")
async def rag_endpoint(query: str):
    context = await search_context(query)
    messages = [
        {"role": "system", "content": f"Use this context: {context}"},
        {"role": "user", "content": query},
    ]
    # Reuse the /chat endpoint above
    return {"messages": messages}
```

## Local LLMs (Ollama)

```python
import httpx
from fastapi.responses import StreamingResponse


class OllamaClient:
    def __init__(self, base_url: str = "http://localhost:11434"):
        self.base_url = base_url

    async def generate(self, model: str, prompt: str) -> str:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/api/generate",
                json={"model": model, "prompt": prompt, "stream": False},
            )
            return response.json()["response"]

    async def generate_stream(self, model: str, prompt: str):
        async with httpx.AsyncClient(timeout=30.0) as client:
            async with client.stream(
                "POST",
                f"{self.base_url}/api/generate",
                json={"model": model, "prompt": prompt, "stream": True},
            ) as response:
                async for line in response.aiter_lines():
                    if line:
                        yield line


ollama = OllamaClient()


@app.post("/local-chat")
async def local_chat(model: str, prompt: str):
    return {"response": await ollama.generate(model, prompt)}


@app.post("/local-stream")
async def local_stream(model: str, prompt: str):
    return StreamingResponse(
        ollama.generate_stream(model, prompt),
        media_type="text/plain",
    )
```

## Errors and Retries

```python
from fastapi import HTTPException
from tenacity import retry, stop_after_attempt, wait_exponential


@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=8))
async def call_llm_with_retry(prompt: str):
    # Provider call
    return prompt


@app.post("/safe")
async def safe_endpoint(prompt: str):
    try:
        return {"response": await call_llm_with_retry(prompt)}
    except Exception:
        raise HTTPException(503, "AI service unavailable")
```

## Best Practices

- Validate inputs with Pydantic.
- Use streaming for UX, but moderate partial outputs.
- Cache responses/embeddings to reduce cost.
- Isolate providers behind a service layer.
- Store keys in environment variables.

## References

- [Responses API](https://platform.openai.com/docs/guides/responses-vs-chat-completions)
- [Streaming Responses](https://platform.openai.com/docs/guides/streaming-responses)
- [OpenAI Security Tools](https://platform.openai.com/docs)

## Next Steps

- [Security Hardening](./18-security-hardening.md) - Protect AI endpoints
- [Observability](./19-observability.md) - Trace and monitor LLM calls

---

[Previous: Project](./16-project-todo.md) | [Back to Index](./README.md) | [Next: Security Hardening](./18-security-hardening.md)
