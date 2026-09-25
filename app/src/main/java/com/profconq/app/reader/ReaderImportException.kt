package com.profconq.app.reader

class ReaderImportException(val reason: Reason) : Exception(reason.name) {
    enum class Reason {
        DOC_NOT_SUPPORTED,
        DOCX_READ_FAILED,
        FILE_READ_FAILED,
    }
}
