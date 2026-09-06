package net.nowhereatall.xfty.lookup;

/**
 * Optional companion to {@link ProviderLookup}. A project whose Providers
 * reference shared ancestors implements this on its lookup too, so those shared
 * ancestors have a default configuration and the Providers work without every
 * test registering them by hand.
 */
public interface SharedAncestorDefaults {

    void registerSharedAncestorDefaults();
}
