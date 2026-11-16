package com.mathtutor.common.port;

import com.mathtutor.common.dto.NoteChunk;
import java.util.List;

/** @brief UC-14: sincroniza únicamente los apuntes incluidos en el código fuente. */
public interface NoteIndex {
    /** @param chunks Instantánea completa validada; vacía elimina los apuntes gestionados. */
    void synchronize(List<NoteChunk> chunks);
}
