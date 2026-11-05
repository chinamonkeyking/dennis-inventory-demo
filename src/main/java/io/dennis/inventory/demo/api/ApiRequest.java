package io.dennis.inventory.demo.api;

public record ApiRequest<T>(String requestId, T params) {
}
