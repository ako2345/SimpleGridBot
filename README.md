# Сеточный бот для Tinkoff Invest Robot Contest

## Пререквизиты
- Java версии не ниже 11
- Gradle версии не ниже 5.0
- Наличие учётной записи в Тинькофф Инвестиции (маржинальная торговля не обязательна)

## Описание стратегии
Бот покупает актив при снижении цены и продаёт при её росте. Это достигается путём разбиения заданного ценового
диапазона на несколько уровней и отслеживания пересечения ценой этих уровней.

При инициализаци бот покупает некоторое количество актива и начинает следить за изменением цены. При превышении
ценой определённых уровней происходит продажа части активов, при обратном движении цены осуществляется покупка
активов.

## Реализация
В проекте представлены два способа реализации стратегии.

В ветке bot-makes-limit-orders торговый робот создаёт ордера для каждого ценового уровня и следит за их исполнением
с помощью стрима ордеров, после чего актуализирует список поручений. Однако стрим ордеров недоступен в режиме
"песочницы" и его пришлось эмулировать, чтобы соответствовать условиям конкурса.

В ветке bot-listens-prices торговый робот следит за ценой инструмента и при пересечении ценового уровня создаёт ордер
на покупку или продажу по рыночной цене. Этот способ приносит меньше прибыли из-за исполнения ордера не по значению
пересечённого ценового уровня.

## Конфигурация
```yaml
app:
  config:
    app-name: ako2345.SimpleGridBot
    sandbox-mode: true
    real-token:
    real-account:
    sandbox-token:
    sandbox-account:

server:
  port: ${PORT:5000}
```
- app-name – имя приложения.
- sandbox-mode – флаг режима "песочница".
- real-token – токен реального пользователя (заполнить, если sandbox-mode: false).
- real-account – идентификатор реальной учётной записи (заполнить, если sandbox-mode: false).
- sandbox-token – токен пользователя в "песочнице" (заполнить, если sandbox-mode: true).
- sandbox-account – идентификатор учётной записи в "песочнице" (заполнить, если sandbox-mode: true).
- port – порт приложения.

## Эндпойнты
После запуска приложения будут доступны следующие эндпойнты:
- POST http://localhost:5000/grid_bot/backtest – проверка бота на исторических данных. Пример конфигураци представлен
  ниже.
- POST http://localhost:5000/grid_bot/init – инициализация бота. Пример конфигураци представлен
  ниже.
- POST http://localhost:5000/grid_bot/close – остановка бота. Пример конфигураци представлен ниже.
- POST http://localhost:5000/grid_bot/analyze – поиск оптимальных параметров бота на исторических данных. Параметр figi
  – идентификатор инструмента (FIGI).

### Пример конфигурации для проверки (backtest) бота
```json
{
  "gridBotConfig": {
    "figi": "BBG000B9XRY4",
    "lowerPrice": 130,
    "upperPrice": 180,
    "gridsNumber": 27,
    "investment": 1000000
  },
  "days": 30,
  "fee": 0.00025
}
```
- figi – идентификатор инструмента (FIGI).
- lowerPrice – нижняя цена ценовой сетки.
- upperPrice – верхняя цена ценовой сетки.
- gridsNumber – количество ценовых уровней сетки.
- investment – размер инвестиций.
- days – количество дней для backtest.
- fee – размер комиссии.

### Пример конфигурации для инициализации бота
```json
{                                                                      
    "figi": "BBG000B9XRY4",                                            
    "lowerPrice": 150,                                                 
    "upperPrice": 180,
    "gridsNumber": 7,
    "investment": 1000000
}
```
- figi – идентификатор инструмента (FIGI).
- lowerPrice – нижняя цена ценовой сетки.
- upperPrice – верхняя цена ценовой сетки.
- gridsNumber – количество ценовых уровней сетки.
- investment – размер инвестиций.

### Пример конфигурации для остановки бота
```json
{                                                                      
    "instrumentShouldBeSold": true
}
```
- instrumentShouldBeSold – флаг, указывающий нужно ли продавать купленный ботом инструмент по текущей цене.

### Пример конфигурации для анализа инструмента
```json
{
  "figi": "BBG000B9XRY4",
  "days": 30,
  "fee": 0.00025
}
```
- figi – идентификатор инструмента (FIGI).
- days – количество дней для backtest.
- fee – размер комиссии.

## Планы
- поддержка одновременной работы нескольких ботов.
- обеспечить корректную работу между торговыми сессиями.
- реализовать стоп-лосс и тэйк-профит.

## PS
Не судите строго, это моё первое приложение на фреймворке Spring!