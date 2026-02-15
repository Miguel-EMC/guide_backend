# Request Bodies, Forms, and Files

This guide covers JSON request bodies, multiple body parameters, form data, file uploads, and how to combine them in FastAPI.

## Prerequisites

```bash
# With uv
uv add "fastapi[standard]"
uv add python-multipart

# With pip
pip install "fastapi[standard]" python-multipart
```

`python-multipart` is required for `Form` and `File` inputs.

## JSON Request Bodies

FastAPI uses Pydantic models to validate JSON request bodies.

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()


class Item(BaseModel):
    name: str
    description: str | None = None
    price: float
    tax: float | None = None


@app.post("/items/")
async def create_item(item: Item):
    return item
```

FastAPI recommends sending request bodies with `POST`, `PUT`, `PATCH`, or `DELETE`. A body in a `GET` request is discouraged by the HTTP spec (FastAPI supports it for edge cases, but Swagger UI won’t document it).

### Body + Path + Query

FastAPI detects where data comes from based on types and path parameters.

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()


class Item(BaseModel):
    name: str
    description: str | None = None
    price: float
    tax: float | None = None


@app.put("/items/{item_id}")
async def update_item(item_id: int, item: Item, q: str | None = None):
    result = {"item_id": item_id, **item.model_dump()}
    if q:
        result["q"] = q
    return result
```

FastAPI will take path parameters from the URL, singular types as query parameters, and Pydantic models from the body.

## Multiple Body Parameters

You can accept multiple body objects; FastAPI will expect a JSON object keyed by parameter name.

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()


class Item(BaseModel):
    name: str
    description: str | None = None
    price: float
    tax: float | None = None


class User(BaseModel):
    username: str
    full_name: str | None = None


@app.put("/items/{item_id}")
async def update_item(item_id: int, item: Item, user: User):
    return {"item_id": item_id, "item": item, "user": user}
```

Expected JSON:

```json
{
  "item": {"name": "Foo", "price": 42.0},
  "user": {"username": "dave"}
}
```



### Singular Values in the Body

Use `Body()` to force a single value into the body instead of query params.

```python
from typing import Annotated
from fastapi import Body, FastAPI
from pydantic import BaseModel

app = FastAPI()


class Item(BaseModel):
    name: str
    price: float


class User(BaseModel):
    username: str


@app.put("/items/{item_id}")
async def update_item(
    item_id: int,
    item: Item,
    user: User,
    importance: Annotated[int, Body()],
):
    return {"item_id": item_id, "item": item, "user": user, "importance": importance}
```



### Embed a Single Body Parameter

By default, a single model is expected at the root. Use `embed=True` to nest it under a key.

```python
from typing import Annotated
from fastapi import Body, FastAPI
from pydantic import BaseModel

app = FastAPI()


class Item(BaseModel):
    name: str
    price: float


@app.put("/items/{item_id}")
async def update_item(item_id: int, item: Annotated[Item, Body(embed=True)]):
    return {"item_id": item_id, "item": item}
```



## Form Data

When the client sends form-encoded data, use `Form`. It requires `python-multipart`.

```python
from typing import Annotated
from fastapi import FastAPI, Form

app = FastAPI()


@app.post("/login/")
async def login(
    username: Annotated[str, Form()],
    password: Annotated[str, Form()],
):
    return {"username": username}
```

HTML forms use `application/x-www-form-urlencoded`, and forms with files use `multipart/form-data`.

You can declare multiple `Form` parameters, but you cannot combine `Form`/`File` with JSON `Body` in the same request because of the content type.

## File Uploads

Use `File` for file bodies and `UploadFile` for streaming uploads.

```python
from typing import Annotated
from fastapi import FastAPI, File, UploadFile

app = FastAPI()


@app.post("/files/")
async def upload_bytes(file: Annotated[bytes, File()]):
    return {"file_size": len(file)}


@app.post("/uploadfile/")
async def upload_file(file: UploadFile):
    return {"filename": file.filename}
```

If you declare the parameter as `bytes`, FastAPI reads the whole file into memory, which is fine for small files. `UploadFile` uses a spooled file (in-memory up to a limit, then disk) and is better for larger uploads.

`UploadFile` exposes metadata like `filename`, `content_type`, and the underlying file object via `file`.

### Multiple Files

```python
from typing import Annotated
from fastapi import FastAPI, File, UploadFile

app = FastAPI()


@app.post("/upload-multiple/")
async def upload_multiple(files: Annotated[list[UploadFile], File()]):
    return {"filenames": [f.filename for f in files]}
```



### Forms + Files

You can combine file and form fields in the same request.

```python
from typing import Annotated
from fastapi import FastAPI, File, Form, UploadFile

app = FastAPI()


@app.post("/files/")
async def create_file(
    file: Annotated[bytes, File()],
    fileb: Annotated[UploadFile, File()],
    token: Annotated[str, Form()],
):
    return {
        "file_size": len(file),
        "token": token,
        "fileb_content_type": fileb.content_type,
    }
```

These requests are sent as form data and can mix `bytes` and `UploadFile`.

## Saving Uploaded Files (Streaming)

```python
from pathlib import Path
import shutil
from fastapi import UploadFile


async def save_upload(upload_dir: Path, file: UploadFile) -> Path:
    upload_dir.mkdir(parents=True, exist_ok=True)
    dest = upload_dir / file.filename

    with dest.open("wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    return dest
```

## Summary

| Input Type | Declaration | Content-Type |
|------------|-------------|--------------|
| JSON body | Pydantic model | `application/json` |
| Form fields | `Form()` | `application/x-www-form-urlencoded` |
| Files | `File()` / `UploadFile` | `multipart/form-data` |

## References

- [Request Body](https://fastapi.tiangolo.com/tutorial/body/)
- [Body - Multiple Parameters](https://fastapi.tiangolo.com/tutorial/body-multiple-params/)
- [Form Data](https://fastapi.tiangolo.com/tutorial/request-forms/)
- [Request Files](https://fastapi.tiangolo.com/tutorial/request-files/)
- [Request Forms and Files](https://fastapi.tiangolo.com/tutorial/request-forms-and-files/)

## Next Steps

- [Response Models](./05-response-models.md) - Configure output data
- [Error Handling](./06-error-handling.md) - Handle errors gracefully

---

[Previous: Data Validation](./03-data-validation.md) | [Back to Index](./README.md) | [Next: Response Models](./05-response-models.md)
