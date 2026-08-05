package org.babyfish.jimmer.sql.model.trigger;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DiscriminatorValue("KIND_A")
public interface TriggerKindA extends TriggerDiscriminatorPositionEntity {
}
