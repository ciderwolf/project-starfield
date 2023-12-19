export class AsyncCache<TCacheEntity, TUpdate> {
  private cache: Map<string, TCacheEntity> = new Map<string, TCacheEntity>();
  private loader: (key: string) => Promise<TCacheEntity>;
  private writer: (key: string, value: TUpdate) => Promise<TCacheEntity>;

  constructor(constructor: AsyncCacheConstructor<TCacheEntity, TUpdate>) {
    this.loader = constructor.load;
    this.writer = constructor.write;
  }

  public async get(key: string): Promise<TCacheEntity> {
    if (this.cache.has(key)) {
      return this.cache.get(key)!;
    }
    else {
      const value = await this.loader(key);
      this.cache.set(key, value);
      return value;
    }
  }

  public async set(key: string, value: TUpdate): Promise<TCacheEntity> {
    const result = await this.writer(key, value);
    this.cache.set(key, result);
    return result;
  }

  public put(key: string, value: TCacheEntity): void {
    this.cache.set(key, value);
  }
}

interface AsyncCacheConstructor<TCacheEntity, TUpdate> {
  load: (key: string) => Promise<TCacheEntity>;
  write: (key: string, value: TUpdate) => Promise<TCacheEntity>;
}

type AsyncConstructorFunction<TEntity, TUpdate> = () => AsyncCacheConstructor<TEntity, TUpdate>

export function createAsyncCache<TCacheEntity, TUpdate>(ctor: AsyncConstructorFunction<TCacheEntity, TUpdate>) {
  const cacheStorer = (() => {
    let cache: AsyncCache<TCacheEntity, TUpdate>;
  
    function returnCache() {
      if (!cache) {
        cache = new AsyncCache<TCacheEntity, TUpdate>(ctor());
      }
      return cache;
    }
  
    return returnCache;
  })();

  return cacheStorer;
}

export function createLocalCache<TCacheEntity>(ctor: () => Map<string, TCacheEntity>) {
  const cacheStorer = (() => {
    let cache: Map<string, TCacheEntity>;

    function returnCache() {
      if (!cache) {
        cache = ctor();
      }
      return cache;
    }

    return returnCache;
  })();

  return cacheStorer;
}