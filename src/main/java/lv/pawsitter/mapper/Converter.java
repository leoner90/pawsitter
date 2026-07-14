package lv.pawsitter.mapper;
/**
 * Generic converter interface for transforming between entities and DTOs.
 *
 * <p>This interface defines a bidirectional mapping contract used throughout
 * the application to convert between persistence-layer entities and
 * API-layer Data Transfer Objects (DTOs).</p>
 *
 * @param <Entity> the entity type
 * @param <EntityCreateDTO> the DTO type used for creation requests
 * @param <DTO> the DTO type used for API responses
 */
public interface Converter<Entity, EntityCreateDTO, DTO> {
    /**
     * Converts an entity into a DTO suitable for API responses.
     *
     * @param entity the entity to convert
     * @return the corresponding DTO
     */
    DTO entityToDto(Entity entity);
    /**
     * Converts a creation DTO into a new entity instance.
     *
     * @param dto the DTO containing creation data
     * @return the new entity
     */
    Entity dtoToEntity(EntityCreateDTO dto);
}