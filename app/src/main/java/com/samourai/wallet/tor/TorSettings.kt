package com.samourai.wallet.tor

import io.matthewnelson.topl_core_base.TorSettings

/**
 * samourai-wallet-android
 *
 * @author Sarath
 */
class TorSettings : TorSettings() {

    override val dormantClientTimeout: Int?
        get() = DEFAULT__DORMANT_CLIENT_TIMEOUT

    override val disableNetwork: Boolean
        get() = DEFAULT__DISABLE_NETWORK

    override val dnsPort: String
        get() = PortOption.DISABLED

    override val dnsPortIsolationFlags: List<@IsolationFlag String>?
        get() = arrayListOf(
                IsolationFlag.ISOLATE_CLIENT_PROTOCOL
        )

    override val customTorrc: String?
        get() = null

    override val entryNodes: String?
        get() = DEFAULT__ENTRY_NODES

    override val excludeNodes: String?
        get() = DEFAULT__EXCLUDED_NODES

    override val exitNodes: String?
        get() = DEFAULT__EXIT_NODES

    override val httpTunnelPort: String
        get() = PortOption.DISABLED

    override val httpTunnelPortIsolationFlags: List<@IsolationFlag String>?
        get() = arrayListOf(
                IsolationFlag.ISOLATE_CLIENT_PROTOCOL
        )

    override val listOfSupportedBridges: List<@SupportedBridgeType String>
        get() = arrayListOf(SupportedBridgeType.MEEK, SupportedBridgeType.OBFS4)

    override val proxyHost: String?
        get() = DEFAULT__PROXY_HOST

    override val proxyPassword: String?
        get() = DEFAULT__PROXY_PASSWORD

    override val proxyPort: Int?
        get() = null

    override val proxySocks5Host: String?
        get() = DEFAULT__PROXY_SOCKS5_HOST

    override val proxySocks5ServerPort: Int?
        get() = null

    override val proxyType: @ProxyType String
        get() = ProxyType.DISABLED

    override val proxyUser: String?
        get() = DEFAULT__PROXY_USER

    override val reachableAddressPorts: String
        get() = DEFAULT__REACHABLE_ADDRESS_PORTS

    override val relayNickname: String?
        get() = DEFAULT__RELAY_NICKNAME

    override val relayPort: String
        get() = PortOption.DISABLED

    override val socksPort: String
        get() = PortOption.AUTO

    override val socksPortIsolationFlags: List<@IsolationFlag String>?
        get() = arrayListOf(
                IsolationFlag.KEEP_ALIVE_ISOLATE_SOCKS_AUTH,
                IsolationFlag.IPV6_TRAFFIC,
                IsolationFlag.PREFER_IPV6,
                IsolationFlag.ISOLATE_CLIENT_PROTOCOL
        )

    override val virtualAddressNetwork: String?
        get() = "10.192.0.2/10"

    override val hasBridges: Boolean
        get() = DEFAULT__HAS_BRIDGES

    override val connectionPadding: @ConnectionPadding String
        get() = ConnectionPadding.OFF

    override val hasCookieAuthentication: Boolean
        get() = DEFAULT__HAS_COOKIE_AUTHENTICATION

    override val hasDebugLogs: Boolean
        get() = DEFAULT__HAS_DEBUG_LOGS

    override val hasDormantCanceledByStartup: Boolean
        get() = DEFAULT__HAS_DORMANT_CANCELED_BY_STARTUP

    override val hasOpenProxyOnAllInterfaces: Boolean
        get() = DEFAULT__HAS_OPEN_PROXY_ON_ALL_INTERFACES

    override val hasReachableAddress: Boolean
        get() = DEFAULT__HAS_REACHABLE_ADDRESS

    override val hasReducedConnectionPadding: Boolean
        get() = DEFAULT__HAS_REDUCED_CONNECTION_PADDING

    override val hasSafeSocks: Boolean
        get() = DEFAULT__HAS_SAFE_SOCKS

    override val hasStrictNodes: Boolean
        get() = DEFAULT__HAS_STRICT_NODES

    override val hasTestSocks: Boolean
        get() = DEFAULT__HAS_TEST_SOCKS

    override val isAutoMapHostsOnResolve: Boolean
        get() = DEFAULT__IS_AUTO_MAP_HOSTS_ON_RESOLVE

    override val isRelay: Boolean
        get() = DEFAULT__IS_RELAY

    override val runAsDaemon: Boolean
        get() = DEFAULT__RUN_AS_DAEMON

    override val transPort: String
        get() = PortOption.DISABLED

    override val transPortIsolationFlags: List<@IsolationFlag String>?
        get() = arrayListOf(
                IsolationFlag.ISOLATE_CLIENT_PROTOCOL
        )

    override val useSocks5: Boolean
        get() = DEFAULT__USE_SOCKS5
}