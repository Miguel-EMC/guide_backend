# File Uploads

NestJS uses Multer under the hood for handling `multipart/form-data` uploads. This chapter covers local storage and validation.

## Goals

- Accept single and multiple file uploads
- Validate file size and type
- Store files safely

## Install

```bash
npm install @nestjs/platform-express
```

## Single File Upload

```typescript
// src/files/files.controller.ts
import {
  Controller,
  Post,
  UploadedFile,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';

@Controller('files')
export class FilesController {
  @Post('avatar')
  @UseInterceptors(FileInterceptor('file'))
  upload(@UploadedFile() file: Express.Multer.File) {
    return {
      originalName: file.originalname,
      size: file.size,
      mimeType: file.mimetype,
    };
  }
}
```

## Storage Configuration

```typescript
import { diskStorage } from 'multer';
import { extname } from 'path';

FileInterceptor('file', {
  storage: diskStorage({
    destination: './uploads',
    filename: (_req, file, cb) => {
      const unique = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
      cb(null, `${unique}${extname(file.originalname)}`);
    },
  }),
});
```

## File Validation

```typescript
import {
  FileTypeValidator,
  MaxFileSizeValidator,
  ParseFilePipe,
} from '@nestjs/common';

@UseInterceptors(FileInterceptor('file'))
@Post('avatar')
upload(
  @UploadedFile(
    new ParseFilePipe({
      validators: [
        new MaxFileSizeValidator({ maxSize: 2 * 1024 * 1024 }),
        new FileTypeValidator({ fileType: /(jpg|jpeg|png)$/ }),
      ],
    }),
  )
  file: Express.Multer.File,
) {
  return { ok: true };
}
```

## Multiple Files

```typescript
import { FilesInterceptor } from '@nestjs/platform-express';
import { UploadedFiles } from '@nestjs/common';

@Post('gallery')
@UseInterceptors(FilesInterceptor('files', 5))
uploadMany(@UploadedFiles() files: Express.Multer.File[]) {
  return files.map((file) => file.originalname);
}
```

## Tips

- Validate file types and sizes server-side.
- Store files in a dedicated bucket in production.
- Keep user uploads out of your source tree when deploying containers.

---

[Previous: Rate Limiting](./15-rate-limiting.md) | [Back to Index](./README.md) | [Next: WebSockets ->](./17-websockets.md)
