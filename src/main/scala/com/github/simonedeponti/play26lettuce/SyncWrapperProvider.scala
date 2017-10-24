package com.github.simonedeponti.play26lettuce

import play.api.Configuration
import play.api.cache.SyncCacheApi


class SyncWrapperProvider(val configuration: Configuration, val name: String = "default") extends BaseClientProvider[SyncCacheApi] {

  lazy val get: SyncCacheApi = {
    new SyncWrapper(getLettuceApi(name))(ec)
  }

}