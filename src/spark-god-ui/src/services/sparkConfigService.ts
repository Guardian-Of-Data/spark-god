export interface SparkEnvironmentData {
  sparkProperties: Record<string, string>;
  hadoopProperties: Record<string, string>;
  timestamp: number;
}

export class SparkConfigService {
  private configCache: SparkEnvironmentData | null = null;
  private lastFetchTime = 0;
  private readonly CACHE_DURATION = 30000; // 30초 캐시

  async getSparkConfiguration(): Promise<SparkEnvironmentData | null> {
    const now = Date.now();

    // 캐시가 유효하면 캐시된 데이터 반환
    if (this.configCache && (now - this.lastFetchTime) < this.CACHE_DURATION) {
      return this.configCache;
    }

    try {
      // Spark plugin의 config API 호출
      const response = await fetch('/spark-god/config/json/');

      if (!response.ok) {
        console.warn('Spark config API not available:', response.status);
        return null;
      }

      const configData: SparkEnvironmentData = await response.json();

      // 캐시 업데이트
      this.configCache = configData;
      this.lastFetchTime = now;

      return configData;
    } catch (error) {
      console.warn('Failed to fetch Spark configuration:', error);
      return null;
    }
  }

  generateSystemPromptContext(config: SparkEnvironmentData): string {
    const sparkPropertiesSummary = this.formatAllProperties(config.sparkProperties, "Spark");
    const hadoopPropertiesSummary = this.formatAllProperties(config.hadoopProperties, "Hadoop");

    return `
=== 현재 Spark 환경 설정 정보 ===

${sparkPropertiesSummary}

${hadoopPropertiesSummary}

=== 지침 ===
위의 Spark Properties와 Hadoop Properties 설정 정보를 바탕으로 사용자의 질문에 구체적이고 맞춤형 답변을 제공해주세요.
현재 설정된 값들을 정확히 참고하여 최적화 제안, 문제 해결 방안, 성능 튜닝 등을 제시할 수 있습니다.
`;
  }

  private formatAllProperties(properties: Record<string, string>, type: string): string {
    if (!properties || Object.keys(properties).length === 0) {
      return `${type} Properties: 설정 없음`;
    }

    const sortedEntries = Object.entries(properties).sort(([a], [b]) => a.localeCompare(b));
    const propertiesList = sortedEntries
      .map(([key, value]) => `- ${key}: ${value}`)
      .join('\n');

    return `${type} Properties (총 ${Object.keys(properties).length}개):
${propertiesList}`;
  }
}

export const sparkConfigService = new SparkConfigService();