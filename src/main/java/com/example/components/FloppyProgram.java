package com.example.components;

import com.mojang.serialization.Codec;

import java.util.List;

public record FloppyProgram(byte[] data) {

    private static final Codec<byte[]> BYTE_ARRAY_CODEC =
            Codec.list(Codec.BYTE)
                    .xmap(
                            list -> {
                                byte[] bytes = new byte[list.size()];

                                for (int i = 0; i < list.size(); i++) {
                                    bytes[i] = list.get(i);
                                }

                                return bytes;
                            },
                            bytes -> {
                                List<Byte> list = new java.util.ArrayList<>(bytes.length);

                                for (byte b : bytes) {
                                    list.add(b);
                                }

                                return list;
                            }
                    );

    public static final Codec<FloppyProgram> CODEC =
            BYTE_ARRAY_CODEC.xmap(
                    FloppyProgram::new,
                    FloppyProgram::data
            );

    public byte[] copyData() {
        return data.clone();
    }
}
