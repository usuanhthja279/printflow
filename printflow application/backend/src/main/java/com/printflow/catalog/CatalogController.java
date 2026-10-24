package com.printflow.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/catalog")
public class CatalogController {
    public record Product(String code, String name, String description) {}

    public static final List<Product> PRODUCTS = List.of(
        new Product("WEDDING_CARDS", "Shaadi & Wedding Cards", "Wedding invitations and celebration stationery"),
        new Product("HOTEL_PRINT", "Hotel Print Materials", "Menus, table cards and other hotel printing requirements"),
        new Product("OFFSET_GLOSS", "Offset Gloss Printing", "Custom offset print enquiries with gloss finishing requirements"),
        new Product("BROCHURES", "Pamphlets & Leaflets", "Promotional pamphlets, leaflets and brochures"),
        new Product("CARDS", "Business Cards", "Corporate visiting cards and stationery"),
        new Product("BOOKS", "Books & Catalogues", "Multi-page publications and catalogues"),
        new Product("PACKAGING", "Packaging", "Printed sleeves and paperboard boxes"),
        new Product("CUSTOM", "Custom Job", "Describe your requirements and receive a tailored estimate")
    );

    private final ProductTemplates templates;
    public CatalogController(ProductTemplates templates){this.templates=templates;}
    @GetMapping
    public List<Product> get() { return templates.list(false).stream().map(t->new Product(t.code(),t.name(),t.description())).toList(); }
}
