package pw.cris.cuadremos.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("two copies with the same id are the same user")
    void sameIdMeansSameUser() {
        UUID id = UUID.randomUUID();
        User first = User.builder().id(id).username("cris").build();
        User second = User.builder().id(id).username("cris").build();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("users with different ids are different users")
    void differentIdMeansDifferentUser() {
        User cris = User.builder().id(UUID.randomUUID()).username("cris").build();
        User yuka = User.builder().id(UUID.randomUUID()).username("yuka").build();

        assertThat(cris).isNotEqualTo(yuka);
    }

    @Test
    @DisplayName("two unsaved users are never equal even with identical data")
    void unsavedUsersAreNotEqual() {
        User first = User.builder().username("cris").build();
        User second = User.builder().username("cris").build();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isEqualTo(first);
    }

    @Test
    @DisplayName("a user stays findable in a set after receiving its id")
    void staysInSetAfterGettingId() {
        User cris = User.builder().username("cris").build();
        Set<User> members = new HashSet<>();
        members.add(cris);

        cris.setId(UUID.randomUUID()); // what happens when the database saves it

        assertThat(members).contains(cris);
    }
}
