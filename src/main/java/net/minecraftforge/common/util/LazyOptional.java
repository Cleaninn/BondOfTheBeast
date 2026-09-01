package net.minecraftforge.common.util;

public class LazyOptional<T> {
    public static <T> LazyOptional<T> empty() {
        return new LazyOptional<>();
    }

    public void ifPresent(NonNullConsumer<? super T> consumer) {
    }
}