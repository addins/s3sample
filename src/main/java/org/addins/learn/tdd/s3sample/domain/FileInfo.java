package org.addins.learn.tdd.s3sample.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "file_info")
public class FileInfo {
    @Id
    private UUID id;

    private String name;

    private String contentType;

    private String storePath;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        FileInfo fileInfo = (FileInfo) o;
        return getId() != null && Objects.equals(getId(), fileInfo.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

	public FileInfo withId(UUID id) {
		this.id = id;
		return this;
	}

	public FileInfo withName(String name) {
		this.name = name;
		return this;
	}

	public FileInfo withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public FileInfo withStorePath(String storePath) {
		this.storePath = storePath;
		return this;
	}
}
