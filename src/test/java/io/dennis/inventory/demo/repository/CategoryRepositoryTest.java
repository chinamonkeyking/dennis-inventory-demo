package io.dennis.inventory.demo.repository;

import io.dennis.inventory.demo.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void seedsCategoriesWithTheirHierarchy() {
        assertThat(categoryRepository.findAll())
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Food", "Clothes", "Cake", "Shoe", "T-Shirts");

        Category food = categoryRepository.findById(1).orElseThrow();
        Category clothes = categoryRepository.findById(2).orElseThrow();
        Category cake = categoryRepository.findById(3).orElseThrow();
        Category shoe = categoryRepository.findById(4).orElseThrow();
        Category tShirts = categoryRepository.findById(5).orElseThrow();

        assertThat(food.getParentId()).isNull();
        assertThat(clothes.getParentId()).isNull();
        assertThat(cake.getParentId()).isEqualTo(1);
        assertThat(shoe.getParentId()).isEqualTo(2);
        assertThat(tShirts.getParentId()).isEqualTo(2);
    }
}
