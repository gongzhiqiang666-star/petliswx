package com.petlis.config;

import com.petlis.repository.BaseRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import java.util.Optional;

/**
 * Repository工厂配置
 *
 * 配置使��自定义的BaseRepository作为基础Repository实现
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
public class RepositoryConfig<R extends JpaRepository<T, I>, T, I>
        extends JpaRepositoryFactoryBean<R, T, I> {

    /**
     * 构造函数
     */
    public RepositoryConfig(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new CustomJpaRepositoryFactory(entityManager);
    }

    /**
     * 自定义Repository工厂
     */
    private static class CustomJpaRepositoryFactory extends JpaRepositoryFactory {

        public CustomJpaRepositoryFactory(EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            // 使用自定义的BaseRepository作为基础类
            return BaseRepository.class;
        }
    }
}
