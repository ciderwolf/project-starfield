export class Cache<TCacheEntity, TUpdate> {
  private cache: Map<string, TCacheEntity> = new Map<string, TCacheEntity>();
  private loader: (key: string) => Promise<TCacheEntity>;
  private writer: (key: string, value: TUpdate) => Promise<TCacheEntity>;

  constructor(constructor: CacheConstructor<TCacheEntity, TUpdate>) {
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

interface CacheConstructor<TCacheEntity, TUpdate> {
  load: (key: string) => Promise<TCacheEntity>;
  write: (key: string, value: TUpdate) => Promise<TCacheEntity>;
}

type ConstructorFunction<TEntity, TUpdate> = () => CacheConstructor<TEntity, TUpdate>

export function createCache<TCacheEntity, TUpdate>(ctor: ConstructorFunction<TCacheEntity, TUpdate>) {
  const cacheStorer = (() => {
    let cache: Cache<TCacheEntity, TUpdate>;
  
    function returnCache() {
      if (!cache) {
        cache = new Cache<TCacheEntity, TUpdate>(ctor());
      }
      return cache;
    }
  
    return returnCache;
  })();

  return cacheStorer;
}