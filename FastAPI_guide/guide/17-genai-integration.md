# GenAI Integration with FastAPI

This chapter covers how to integrate AI/LLM capabilities into FastAPI applications, a key feature for modern 2026 applications.

## Why FastAPI for GenAI?

FastAPI has become the de-facto framework for AI applications due to:

| Feature | Why It Matters for AI |
|---------|---------------------|
| **Async Support** | Handle multiple LLM calls concurrently |
| **Streaming Responses** | Real-time AI-generated content delivery |
| **Type Safety** | Validate complex AI data structures |
| **Performance** | Low latency for AI-powered applications |
| **Auto Documentation** | Document AI endpoints automatically |

## Setting Up AI Environment

### Install Required Packages

```bash
# Core AI packages
pip install "fastapi[standard]" openai anthropic

# For local LLMs
pip install ollama huggingface-hub

# For vector databases
pip install pinecone-client weaviate-client qdrant-client

# For LangChain integration
pip install langchain langchain-openai

# For data processing
pip install pandas numpy scikit-learn
```

### Environment Configuration

```python
# .env
OPENAI_API_KEY=your_openai_key_here
ANTHROPIC_API_KEY=your_anthropic_key_here
PINECONE_API_KEY=your_pinecone_key
OLLAMA_BASE_URL=http://localhost:11434
```

## Basic OpenAI Integration

### Simple Chat Completion

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
import openai
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="GenAI API", version="1.0.0")

# Request/Response Models
class Message(BaseModel):
    role: str = Field(..., description="System, user, or assistant")
    content: str = Field(..., description="Message content")

class ChatRequest(BaseModel):
    messages: List[Message]
    model: str = Field(default="gpt-4o-mini", description="OpenAI model")
    temperature: float = Field(default=0.7, ge=0, le=2)
    max_tokens: Optional[int] = Field(default=None, ge=1)

class ChatResponse(BaseModel):
    message: Message
    usage: dict
    model: str

@app.post("/chat", response_model=ChatResponse)
async def chat_completion(request: ChatRequest):
    try:
        client = openai.AsyncOpenAI()
        
        response = await client.chat.completions.create(
            model=request.model,
            messages=[{"role": msg.role, "content": msg.content} for msg in request.messages],
            temperature=request.temperature,
            max_tokens=request.max_tokens
        )
        
        return ChatResponse(
            message=Message(
                role=response.choices[0].message.role,
                content=response.choices[0].message.content
            ),
            usage=response.usage.model_dump(),
            model=response.model
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

## Streaming Responses

### Real-time AI Generation

```python
from fastapi.responses import StreamingResponse
import json
import asyncio

class StreamingRequest(BaseModel):
    prompt: str
    model: str = "gpt-4o-mini"
    max_tokens: int = 500

async def generate_stream(prompt: str, model: str):
    client = openai.AsyncOpenAI()
    
    try:
        stream = await client.chat.completions.create(
            model=model,
            messages=[{"role": "user", "content": prompt}],
            stream=True,
            max_tokens=500
        )
        
        for chunk in stream:
            if chunk.choices[0].delta.content is not None:
                yield f"data: {json.dumps({'content': chunk.choices[0].delta.content})}\n\n"
                
        yield f"data: {json.dumps({'done': True})}\n\n"
        
    except Exception as e:
        yield f"data: {json.dumps({'error': str(e)})}\n\n"

@app.post("/stream")
async def stream_chat(request: StreamingRequest):
    return StreamingResponse(
        generate_stream(request.prompt, request.model),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache"}
    )
```

## Vector Database Integration

### Pinecone Example for RAG

```python
import pinecone
from sentence_transformers import SentenceTransformer

# Initialize models
embedder = SentenceTransformer('all-MiniLM-L6-v2')
pinecone.init(api_key="your-key", environment="us-west1-gcp")

class DocumentStore:
    def __init__(self):
        self.index_name = "docs"
        
    async def add_document(self, text: str, metadata: dict):
        """Add document to vector store"""
        embedding = embedder.encode(text).tolist()
        
        await pinecone.Index(self.index_name).upsert(
            vectors=[{
                "id": f"doc_{hash(text)}",
                "values": embedding,
                "metadata": {"text": text, **metadata}
            }]
        )
    
    async def search(self, query: str, top_k: int = 5):
        """Search similar documents"""
        query_embedding = embedder.encode(query).tolist()
        
        results = await pinecone.Index(self.index_name).query(
            vector=query_embedding,
            top_k=top_k,
            include_metadata=True
        )
        
        return results.matches

# Usage in endpoint
doc_store = DocumentStore()

@app.post("/rag-query")
async def rag_query(query: str):
    """Retrieve-augmented generation"""
    # Search relevant documents
    docs = await doc_store.search(query)
    
    # Build context
    context = "\n".join([match["metadata"]["text"] for match in docs])
    
    # Generate response with context
    messages = [
        {"role": "system", "content": f"Use this context: {context}"},
        {"role": "user", "content": query}
    ]
    
    # ... (use chat completion endpoint)
```

## LangChain Integration

### Chain Management

```python
from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from langchain.chains import LLMChain
from langchain.schema import BaseOutputParser

class CustomOutputParser(BaseOutputParser):
    def parse(self, text: str) -> dict:
        # Custom parsing logic
        return {"result": text.strip()}

class LangChainService:
    def __init__(self):
        self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)
        self.parser = CustomOutputParser()
        
    def create_chain(self, template: str):
        prompt = ChatPromptTemplate.from_template(template)
        return LLMChain(llm=self.llm, prompt=prompt, output_parser=self.parser)

langchain_service = LangChainService()

@app.post("/chain")
async def run_chain(template: str, inputs: dict):
    chain = langchain_service.create_chain(template)
    result = await chain.arun(**inputs)
    return result
```

## Local LLM Integration

### Ollama Setup

```python
import httpx
from typing import AsyncGenerator

class OllamaClient:
    def __init__(self, base_url: str = "http://localhost:11434"):
        self.base_url = base_url
        
    async def generate(self, model: str, prompt: str) -> str:
        """Generate response from local model"""
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/api/generate",
                json={
                    "model": model,
                    "prompt": prompt,
                    "stream": False
                }
            )
            return response.json()["response"]
    
    async def generate_stream(self, model: str, prompt: str) -> AsyncGenerator[str, None]:
        """Stream response from local model"""
        async with httpx.AsyncClient(timeout=30.0) as client:
            async with client.stream(
                "POST",
                f"{self.base_url}/api/generate",
                json={"model": model, "prompt": prompt, "stream": True}
            ) as response:
                async for line in response.aiter_lines():
                    if line:
                        chunk = json.loads(line)
                        if "response" in chunk:
                            yield chunk["response"]

ollama = OllamaClient()

@app.post("/local-chat")
async def local_chat(model: str, prompt: str):
    """Chat with local LLM"""
    response = await ollama.generate(model, prompt)
    return {"response": response}

@app.post("/local-stream")
async def local_stream(model: str, prompt: str):
    """Stream from local LLM"""
    return StreamingResponse(
        ollama.generate_stream(model, prompt),
        media_type="text/plain"
    )
```

## Error Handling & Rate Limiting

### Robust AI Error Management

```python
from fastapi import HTTPException, status
from tenacity import retry, stop_after_attempt, wait_exponential
import logging

logger = logging.getLogger(__name__)

class AIServiceError(Exception):
    """Custom AI service error"""
    pass

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10)
)
async def call_llm_with_retry(prompt: str):
    """LLM call with exponential backoff"""
    try:
        # AI API call here
        pass
    except openai.RateLimitError:
        raise AIServiceError("Rate limit exceeded")
    except openai.APITimeoutError:
        raise AIServiceError("Request timeout")
    except Exception as e:
        logger.error(f"LLM error: {str(e)}")
        raise AIServiceError(f"AI service error: {str(e)}")

@app.exception_handler(AIServiceError)
async def ai_exception_handler(request, exc):
    return JSONResponse(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        content={"error": "AI service temporarily unavailable", "detail": str(exc)}
    )
```

## Performance Optimization

### Caching & Connection Pooling

```python
from functools import lru_cache
import aioredis

# Redis for caching
redis = aioredis.from_url("redis://localhost")

@lru_cache(maxsize=1000)
def cached_embedding(text: str):
    """Cache embeddings to avoid recomputation"""
    return embedder.encode(text).tolist()

@app.post("/smart-cache")
async def smart_chat(prompt: str):
    """Chat with intelligent caching"""
    # Check cache first
    cache_key = f"chat:{hash(prompt)}"
    cached_response = await redis.get(cache_key)
    
    if cached_response:
        return json.loads(cached_response)
    
    # Generate new response
    response = await generate_ai_response(prompt)
    
    # Cache for 1 hour
    await redis.setex(cache_key, 3600, json.dumps(response))
    
    return response
```

## Testing AI Applications

### Unit Testing with Mocks

```python
import pytest
from unittest.mock import AsyncMock, patch
from fastapi.testclient import TestClient

client = TestClient(app)

@pytest.mark.asyncio
async def test_chat_endpoint():
    """Test chat completion with mocked OpenAI"""
    mock_response = {
        "choices": [{
            "message": {"role": "assistant", "content": "Test response"},
            "delta": None
        }],
        "usage": {"total_tokens": 100},
        "model": "gpt-4o-mini"
    }
    
    with patch('openai.AsyncOpenAI') as mock_openai:
        mock_client = AsyncMock()
        mock_client.chat.completions.create.return_value = mock_response
        mock_openai.return_value = mock_client
        
        response = client.post("/chat", json={
            "messages": [{"role": "user", "content": "Hello"}]
        })
        
        assert response.status_code == 200
        assert response.json()["message"]["content"] == "Test response"
```

## Deployment Considerations

### Production Checklist

| Concern | Solution |
|---------|----------|
| **API Keys** | Use environment variables and secret management |
| **Rate Limiting** | Implement both client and API-level limits |
| **Scaling** | Use async patterns and connection pooling |
| **Monitoring** | Track token usage, latency, and error rates |
| **Cost Control** | Set limits on tokens and concurrent requests |
| **Data Privacy** | Handle PHI/PII according to regulations |

## Best Practices

1. **Always validate inputs** - Use Pydantic models
2. **Implement rate limiting** - Protect against abuse
3. **Cache responses** - Reduce API costs
4. **Use streaming** - Better user experience
5. **Monitor usage** - Track costs and performance
6. **Have fallbacks** - Multiple AI providers
7. **Secure keys** - Never commit API keys
8. **Log appropriately** - Don't log sensitive data

## Next Steps

- [Testing](./14-testing.md) - Test your AI applications
- [Deployment](./15-deployment.md) - Deploy AI-powered APIs
- [Architecture](./13-architecture.md) - Scale AI applications

---

[Back to Index](./README.md) | [Next: Testing](./14-testing.md)