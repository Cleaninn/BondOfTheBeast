# Changelog

## 1.1.0-beta_23 - Minecraft 1.20.1 / Fabric

- Пыль лунного забвения действует 2,5 секунды и позволяет небольшие уклонения. Для принудительного надевания ошейника нужно удерживать ПКМ и прицел на игроке 2 секунды; прерывание не расходует ошейник.
- Занятый слот шеи допускается: прежний предмет возвращается в инвентарь игрока либо выпадает рядом.
- Сопротивление доступно раз в 5 минут с момента активации; оставшееся время сохраняется и показывается в интерфейсе.
- Принудительная связь оставляет сидение, запреты блоков, взаимодействий и атак, управление бронёй, поводок и лежанку. Боевые усиления, отзыв и поглощение доступны через добровольный контракт.
- Сон на лежанке добавляет 0,1 инстинкта в секунду на стадиях 0–2; во время сопротивления рост приостанавливается.

## 1.1.0-beta_22 - Minecraft 1.20.1 / Fabric

- Иконки способностей, брони, правил блоков и возврата уменьшены: вокруг значков больше свободного места, пиксели остаются чёткими и квадратными.

## 1.1.0-beta_21 - Minecraft 1.20.1 / Fabric

- Шкала под моделью питомца помещена в родную рамку текстуры гримуара. Заполнение совпадает с рамкой при изменении размера окна и масштаба интерфейса.
- Подпись «До усиления связи» сдвинута ниже, чтобы не перекрывать шкалу.

## 1.1.0-beta_20 - Minecraft 1.20.1 / Fabric

- Все экраны проверены на маленьком и большом окне. Подписи кнопок ограничены по ширине и высоте; мелкий книжный шрифт заменён читаемым ванильным. Подсказки удерживаются внутри экрана.
- Контракт и настройки лежанки оформлены как страницы гримуара. Список питомцев у лежанки получил ограниченную высоту, прокрутку и перелистывание.
- Редактор блоков подстраивает число строк под окно; поиск и кнопки находятся внутри рамки, пустые клетки технических блоков убраны. Добавлены полоса прокрутки и сообщение при пустом поиске.
- В окне связи и экранном индикаторе длинные пояснения переносятся на строки. Заголовок брони ограничен рамкой, пустой список питомцев читается на пергаменте.
- Плашки полной связи и насильного приручения используют короткие подписи. Переход к дневнику из книги SSC ограничен границами окна.

## 1.1.0-beta_19 - Minecraft 1.20.1 / Fabric

- Сидение обозначено сидящим животным; запрет разрушения — блоком с минусом, запрет взаимодействий — рукой с минусом. Все новые значки нарисованы на сетке 16×16.
- Значок возврата питомца сдвинут влево на два экранных пикселя. Нижняя кнопка «Готово» заменена квадратной кнопкой назад справа сверху на обложке гримуара.
- Вместо остатка опыта под шкалой показана компактная плашка «До усиления связи» с оставшимся временем. Текст подгоняется по ширине рамки.

## 1.1.0-beta_18 - Minecraft 1.20.1 / Fabric

- Поле иконки на кнопках сделано квадратным; добавлена бордовая кожаная подложка, рамка и тень. Значки увеличены без искажения пикселей.
- Новый стиль: светлая гравировка с бронзовыми акцентами. Прежний стиль сохранён в textures/gui/abilities/ink и history/interface-beta_17-before-beta_18.

## 1.1.0-beta_17 - Minecraft 1.20.1 / Fabric

- Все 14 значков гримуара перерисованы в стиле книжных пиктограмм: тёмные чернила и золотистые акценты. Броня и правила блоков используют собственные иконки вместо предметов Minecraft.
- Исправлен дробный масштаб текстур: каждый пиксель иконки отображается целым квадратом экранных пикселей; положение выровнено по экранной сетке.

## 1.1.0-beta_16 - Minecraft 1.20.1 / Fabric

- Все десять иконок 16×16 переделаны в стиле предметов: единая тёмная окантовка толщиной один пиксель, светлые грани и небольшая палитра материалов.
- Контуры замкнуты на диагоналях; выровнены пропорции и поля. Сидение обозначено мордой волка, запрет взаимодействий — рычагом.

## 1.1.0-beta_15 - Minecraft 1.20.1 / Fabric

- Иконки 16×16 заменены простыми крупными символами с плоской заливкой: стул, стрелка возврата, щит, лапа, капля, кирка, рука, меч, книга и замок.
- Убраны мелкие эмблемы, шерсть, орнаменты и лишние оттенки. Запреты отмечены одинаковой красной чертой.

## 1.1.0-beta_14 - Minecraft 1.20.1 / Fabric

- Все десять иконок способностей вручную перерисованы на сетке 16×16 в стиле предыдущего набора 32×32: силуэты и мелкие детали упрощены для читаемости.
- Рендер использует текстуры 16×16 без сглаживания. Пиксельные рисунки сохранены в воспроизводимом скрипте tools/draw_ability_sprites16.py.

## 1.1.0-beta_13 - Minecraft 1.20.1 / Fabric

- Все десять иконок способностей перерисованы отдельно под сетку 32×32: новые силуэты, контуры и детали материалов вместо удвоения старых пикселей.
- Новый импортёр переносит рисунки прямо в 32×32 с общей палитрой и жёсткой прозрачностью, без промежуточного спрайта 16×16.

## 1.1.0-beta_12 - Minecraft 1.20.1 / Fabric

- Все десять используемых текстур способностей увеличены до 32×32 без сглаживания.
- Отрисовка и импорт текстур обновлены под размер 32×32.

## 1.1.0-beta_11 - Minecraft 1.20.1 / Fabric

- Переработаны десять значков способностей: видимые рисунки подогнаны по размеру ванильного нагрудника, лишние прозрачные поля убраны.
- PNG используют жёсткую прозрачность и небольшую общую палитру. Убраны мягкие края и одиночные остаточные пиксели после уменьшения.
- Рендер явно использует фильтрацию ближайшего пикселя; положение иконки округлено до целых координат интерфейса.
- Старые варианты текстур сохранены. Механики и расположение кнопок не менялись.

## 1.1.0-beta_10 - Minecraft 1.20.1 / Fabric

- Добавлены десять отдельных текстур способностей 16×16: простые пиксельные силуэты волка, лап, щита, капли с клыками, инструментов, руки, книги и нагрудника с замком.
- Способности используют собственные текстуры вместо значков обычных предметов. Сохранены крупное левое поле, правый индикатор состояния и подсказки. Навигация к броне и правилам блоков сохраняет предметные значки.
- Разблокировка команд не менялась: добровольная связь развивается по времени игры питомца в ошейнике, принудительная — по индексу стадии трансформации. Числовой уровень и опыт остались от старой системы и сейчас не открывают команды.

## 1.1.0-beta_9 - Minecraft 1.20.1 / Fabric

- Кнопки способностей переработаны по эскизу: крупное поле иконки слева и отдельное узкое поле с углублённым индикатором справа. Сетка — три столбца и четыре ряда.
- Вместо сгенерированных значков используются настоящие предметы Minecraft с небольшими пиксельными отметками запретов и замка. Иконки учитывают активный ресурс-пак.
- Открытие брони оформлено кнопкой с железным нагрудником. Переключатель запрета самостоятельной смены брони отличается кожаным нагрудником с замком.
- Запрет и допуск блоков перенесены в отдельное окно «Правила блоков». Вход — кнопка с досками; из редактора списка можно вернуться по Esc в правила, затем в гримуар.

## 1.1.0-beta_8 - Minecraft 1.20.1 / Fabric

- Команды оформлены прямоугольными бумажными кнопками: слева отдельная иконка, справа углублённый индикатор, зелёный при включении.
- Добавлены десять иконок способностей в единой золотисто-бордовой стилистике. Названия, описания, состояние и причины недоступности показываются при наведении.
- Имя питомца перенесено на кожаную плашку над портретом. Убрана надпись стадии. Под шкалой показан остаток опыта до следующего уровня, на предельном уровне — соответствующая подпись.
- Исправлена поза предпросмотра: используется штатная анимация покоя SSC. Четвероногие формы показаны под небольшим углом в карточках и гримуаре.
- Проверены компиляция, 48 JSON ресурсов и запуск клиента с визуальными снимками при 854×480 и 640×480. Проверка интерфейса использует временные демонстрационные данные; повторная проверка всех игровых механик не выполнялась.

## 1.1.0-beta_1 - Minecraft 1.20.1 / Fabric

- Kept voluntary contracts as the main path, available at the terminal SSC form. Both participants sign; a new bond starts with basic commands rather than every ability.
- Replaced XP purchases with separate voluntary care milestones: hungry feeding and 30 seconds of bed rest, 120 seconds of walking, then 600 seconds of shared activity. Each new command set requires the pet's own confirmation through G. Old purchased abilities migrate without losing ownership.
- Reworked the separate forced path around native SSC stages and instinct. Both collars work on stunned targets; infused collars wait for a curse, warn for 10 seconds and slowly supply catalyst. Ordinary collars have no passive catalyst.
- Added golden-apple release before stage 2, enchanted-apple release at stage 2, and physical-contract release at stage 3. Unique contract tokens prevent stale contracts from releasing a newer forced bond, including offline releases.
- Added a ten-second final-stage warning, two-minute early resistance, bounded sit/absorption commands and persistent one-cookie-per-stage feeding limits. Removed forced proximity punishment.
- Rebuilt management and pet status screens, added the G status key and forced-bond HUD. Server permissions use voluntary trust milestones or forced SSC stages as appropriate.
- Added a separate infused collar model with a purple band, gray buckle and catalyst reservoir, using a vanilla material. Added synchronized armor management and optional owner-only armor control.
- Fixed Russian translation encoding and added a resource-check warning for damaged translations. Updated the final-bond advancement to care milestones.

### Verification

- Gradle build and remapping passed; 48 JSON resources validated with zero errors.
- Isolated dedicated server: 45 forced-taming checks, 29 voluntary-contract/progression checks, 9 warning/timeout/offline-contract checks, 8 lifecycle checks and 3 legacy-migration checks passed (94 total).
- Actual protocol actions covered collars, powder, catalyst feeding, apple eating, commands, armor slots and signed contract destruction. Tests advanced disposable saved counters near thresholds for long milestones and transformation; actual server ticks completed the transitions. Production sources contain no test accelerators.
- Fabric client connected, opened the real management packet screen, synchronized its own equipped collar and displayed the voluntary status screen. Screenshots include a standalone rendering of the actual infused-collar mesh and material; this preview does not prove fit on every SSC model. Temporary client harness classes are excluded from release JARs.
- Upstream SSC/Apoli power-registration and Origins-layer warnings remain in the development dependency stack. Tested server/client flows completed; coverage does not establish compatibility with every SSC form or third-party modpack.

## 1.1.0-beta_2 - Minecraft 1.20.1 / Fabric

- Replaced care chores and pet confirmations with automatic bond time while a pet is alive in its collar: first commands at 30 minutes, magical commands at 2 hours and full powers at 6 hours. Time pauses without the collar or outside the pet's active play; it does not require its owner to stay online.
- Made basic block, interaction and armor restrictions available to the owner as soon as the voluntary bond is signed. Magic commands unlock automatically over time.
- Replaced the flat pet list with paged Grimoire cards showing each pet's last known SSC form, online status and a Select button. A single pet also opens through the card screen.
- Added separate Gradle launch profiles and Windows shortcuts for a local dedicated server, owner client and pet client, including one shortcut that opens all three.
- This is a testing patch. It replaces the previous care-milestone design; player authority over ordinary owner controls now follows the contract directly.

### Verification

- Gradle compilation and resource validation: pending final build.
- Previous 1.1.0-beta_1 integration counts describe the prior mechanic and are not coverage for this patch. No gameplay test suite was rerun for this patch.

## 1.0.3-beta_1 ? Minecraft 1.20.1 / Fabric

- Simplified the infused-collar inventory sprite to a vanilla-style 16?16 purple leather band and gray buckle, removing gemstone and rune details.
- Resource verification accepts standard 16?16 and existing 32?32 item textures.
- Verification: visual inspection of the native sprite and enlarged preview, resource validation and Gradle build. Gameplay Java code is identical to 1.0.2-beta_1; prior integration results apply to that version.

## 1.0.2-beta_1 ? Minecraft 1.20.1 / Fabric

- Replaced bed-chain particles with world-rendered leash geometry. Added owner-held leads and persistent block anchors on fences, walls, iron bars and chains, with survival item consumption and return on release.
- Added missing infused-collar and lunar-oblivion-dust textures, models, recipes and creative entries; implemented the feral collar model and infused-collar rendering.
- Completed forced taming: initial SSC transformation, saved progress, instinct growth, final transformation and golden-apple escape. Both collar types now share the existing pet mechanics.
- Centralized ownership/release rules and offline contract releases. Fixed sleeping on pet beds, waking, owner sleep-bonus lookup, seating movement and absorption game-mode restoration.
- Implemented vampiric healing and shared owner/pet combat protection. Added server validation of skill prerequisites, owner permissions, beds, ranges and block lists; prevented incompatible tether/teleport/absorption combinations.
- Fixed release metadata substitution. Upgraded release number, selected compatible Gradle 8.12 and JDK 21 for CI (Java bytecode remains 17).

### Verification

- Gradle build and remapping; resource checker: 48 JSON files, zero errors.
- Isolated dedicated server with three protocol clients: 38 checks passed, including ownership, unauthorized actions, leads and anchor removal, skills, combat, sitting, beds/sleep, absorption, golden-apple escape and offline contract release.
- Eight persistence/progression checks passed: saved taming, reconnect, final SSC form, infused-collar leads, fixed anchors after reconnect and owner-disconnect handling. The disposable player save was advanced to tick 5990 to exercise completion without waiting five minutes; early progress was observed normally.
- Eight lifecycle checks passed: dimension changes, return to the bed dimension, death and respawn of owner/pet, lead cleanup and ownership retention.
- Fabric client connected to the isolated server, loaded assets and visibly rendered the silver bed tether on a feral pet. Removed a client-side Trinkets inventory check that initially hid confirmed tethers. The screenshot is included with release notes.

Coverage does not establish compatibility with every SSC form or third-party modpack. Client startup reports upstream SSC/Apoli power-registration and Origins-layer warnings in this development dependency stack; they did not stop the tested gameplay or client.
