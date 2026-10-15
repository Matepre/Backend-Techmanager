package com.techmanager.techmanager.service.impl;

import com.techmanager.techmanager.entity.Marca;
import com.techmanager.techmanager.entity.Producto;
import com.techmanager.techmanager.repository.MarcaRepository;
import com.techmanager.techmanager.repository.ProductoRepository;
import com.techmanager.techmanager.service.ProductoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final MarcaRepository marcaRepository;

    public ProductoServiceImpl(
            ProductoRepository productoRepository,
            MarcaRepository marcaRepository
    ) {
        this.productoRepository = productoRepository;
        this.marcaRepository = marcaRepository;
    }

    @Override
    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    @Override
    public Producto guardar(Producto producto) {
        normalizar(producto);
        producto.setMarca(resolverMarca(producto));
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        return productoRepository.save(producto);
    }

    @Override
    public Producto actualizar(Long id, Producto producto) {
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        normalizar(producto);
        existente.setNombre(producto.getNombre());
        existente.setDescripcion(producto.getDescripcion());
        existente.setPrecioCompra(producto.getPrecioCompra());
        existente.setPrecioVenta(producto.getPrecioVenta());
        existente.setStock(producto.getStock());
        existente.setMarca(resolverMarca(producto));
        existente.setActivo(producto.getActivo() == null || producto.getActivo());
        return productoRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        productoRepository.deleteById(id);
    }

    private void normalizar(Producto producto) {
        producto.setNombre(producto.getNombre().trim());
        if (producto.getDescripcion() != null) {
            producto.setDescripcion(producto.getDescripcion().trim());
        }
    }

    private Marca resolverMarca(Producto producto) {
        if (producto.getMarca() == null || producto.getMarca().getId() == null) {
            throw new IllegalArgumentException("La marca es obligatoria");
        }
        return marcaRepository.findById(producto.getMarca().getId())
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));
    }
}