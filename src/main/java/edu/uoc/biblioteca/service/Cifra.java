package edu.uoc.biblioteca.service;

/**
 * Una etiqueta con su valor, para las gráficas y los recuentos del panel: «Novela · 5».
 * Las consultas de agregación la construyen directamente con {@code select new ...Cifra(...)}.
 */
public record Cifra(String etiqueta, long valor) {
}
