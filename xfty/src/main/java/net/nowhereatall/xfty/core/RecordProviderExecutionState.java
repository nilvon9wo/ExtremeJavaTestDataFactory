package net.nowhereatall.xfty.core;

import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.persistence.PersistenceGatewayLike;

/** The parent call's state a child collection needs to generate itself against. */
record RecordProviderExecutionState(
        ProviderLookupLike providerLookup,
        RecordProviderLike factoryOutlet,
        InsertMode insertMode,
        InsertInclusivity inclusivity,
        PersistenceGatewayLike persistenceGateway) {
}
