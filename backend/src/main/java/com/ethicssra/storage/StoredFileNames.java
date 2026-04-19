package com.ethicssra.storage;

/**
 * 从存储引用中解析下载展示文件名（object key 或本地路径均为 {@code <uuid>_<safeName>} 形式）。
 */
public final class StoredFileNames {

    private StoredFileNames() {
    }

    public static String downloadName(String storedRef) {
        if (storedRef == null || storedRef.isBlank()) {
            return "attachment";
        }
        String seg = storedRef;
        int slash = storedRef.lastIndexOf('/');
        if (slash >= 0) {
            seg = storedRef.substring(slash + 1);
        }
        int us = seg.indexOf('_');
        if (us >= 36) {
            return seg.substring(us + 1);
        }
        return seg;
    }
}
