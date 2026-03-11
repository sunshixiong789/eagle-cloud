import { useRef, useState, useEffect } from 'react';

// ----------------------------------------------------------------------

export function useFilePreview(file?: File | string | null): string {
  if (file instanceof File) {
    return URL.createObjectURL(file);
  } else if (typeof file === 'string') {
    return file;
  } else {
    return '';
  }
}

// ----------------------------------------------------------------------

export type FilePreviewItem = {
  previewUrl: string;
  file: File | string;
};

export type UseFilesPreviewReturn = {
  filesPreview: FilePreviewItem[];
  setFilesPreview: React.Dispatch<React.SetStateAction<FilePreviewItem[]>>;
};

export function revokeObjectUrls(urls: string[]) {
  urls.forEach((url) => URL.revokeObjectURL(url));
}

export function useFilesPreview(files: (File | string)[]): UseFilesPreviewReturn {
  const objectUrlsRef = useRef<string[]>([]);
  const [filesPreview, setFilesPreview] = useState<FilePreviewItem[]>([]);

  useEffect(() => {
    // Cleanup old object URLs
    revokeObjectUrls(objectUrlsRef.current);
    objectUrlsRef.current = [];

    const previews: FilePreviewItem[] = files.map((file) => {
      const isFile = file instanceof File;
      const previewUrl = isFile ? URL.createObjectURL(file) : file;

      if (isFile) objectUrlsRef.current.push(previewUrl);

      return {
        file,
        previewUrl,
      };
    });

    setFilesPreview(previews);

    return () => {
      revokeObjectUrls(objectUrlsRef.current);
      objectUrlsRef.current = [];
    };
  }, [files]);

  return {
    filesPreview,
    setFilesPreview,
  };
}
