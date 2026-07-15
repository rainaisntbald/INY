package net.iridiummc.iny.internal.codec;

/** Internal bridge for resolving and decoding values without expanding the public service API. */
public interface InyValueAccess {

    <T> T resolve(Object value, Class<T> type, String path);

    <T> T decode(Object value, Class<T> type, String path);
}
