package org.babyfish.jimmer.sql.model.trigger;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

/**
 * Minimal repro for trigger SELECT column-order bug.
 *
 * <p>SINGLE_TABLE polymorphism with ONE field declared BEFORE
 * {@code @Discriminator}. This is the smallest setup where jimmer's
 * INSERT order (id, type, others) diverges from
 * {@link org.babyfish.jimmer.meta.ImmutableType#getSelectableProps()}
 * declaration order (id, before, type, after).
 *
 * <p>INSERT order: id, kind, before, after (kind forced to col 2).
 * <br>getSelectableProps() order: id, before, kind, after (kind at col 3).
 *
 * <p>{@code MutableUpdateImpl.renderAsSelect} uses getSelectableProps()
 * (BUG), but {@link org.babyfish.jimmer.sql.runtime.ObjectReader} reads
 * the discriminator at col 2 (consistent with INSERT).
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "TRIGGER_DISCRIMINATOR_POSITION_ENTITY")
public interface TriggerDiscriminatorPositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Nullable
    String before();

    @Discriminator
    @Column(name = "KIND")
    String type();

    @Nullable
    String after();
}
