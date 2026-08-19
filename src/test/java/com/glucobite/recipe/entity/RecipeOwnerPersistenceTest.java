package com.glucobite.recipe.entity;

import com.glucobite.user.entity.User;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class RecipeOwnerPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void rejectsRecipeWithoutOwner() {
        Recipe recipe = new Recipe(null, "소유자 없는 레시피", null, 10);

        assertThrows(PersistenceException.class, () ->
                entityManager.persistAndFlush(recipe)
        );
    }

    @Test
    void persistsRecipeWithOwner() {
        User user = entityManager.persistAndFlush(
                new User("recipe-user", "encoded-password", "레시피 사용자")
        );
        Recipe recipe = entityManager.persistAndFlush(
                new Recipe(user, "두부구이", "두부를 노릇하게 굽는다.", 15)
        );

        assertNotNull(recipe.getId());
    }
}
