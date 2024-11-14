package org.example.smarphonebot2.service;

import org.example.smarphonebot2.entity.Model;
import org.example.smarphonebot2.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Model> getProductByBrand(String brandName) {
        return productRepository.findByNameContainingIgnoreCase(brandName);
    }

    public List<Model> getProductByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    public void saveProduct(Model model){
        if (model.getName() == null || model.getPrice() ==  null ||
            model.getDescription() == null || model.getImageFile() == null) {
            throw new IllegalArgumentException("Mahsulot Ma'lumotlari to'liq emas.");
        }
        productRepository.save(model);
    }

    public void addModel(String modelName, double modelPrice, String modelDescription, String modelImages) {
        Model model = new Model();
        model.setName(modelName);
        model.setPrice(BigDecimal.valueOf(modelPrice));
        model.setDescription(modelDescription);
        model.setImageFile(modelImages);
        productRepository.save(model);
    }

    public void deleteModelById(long modelId) {
        productRepository.deleteById(modelId);
    }

    public Model getModelById(long modelId) {
        return productRepository.findById(modelId).orElseThrow();

    }
}
