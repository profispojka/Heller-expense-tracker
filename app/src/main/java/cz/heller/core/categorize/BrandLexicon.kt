package cz.heller.core.categorize

/**
 * GENEROVANÝ SOUBOR — needitovat ručně. Vytváří `tools/lexicon/generate-brands.mjs`.
 *
 * Značky obchodů, restaurací, čerpacích stanic atd. z Name Suggestion Index (OpenStreetMap,
 * licence BSD-3, verze 6.0.20250817), omezené na svět/Evropu + CZ, SK, PL, DE, AT, HU, HR, SI, ME
 * a namapované na kategorie Heller. Klíče jsou normalizované tokenové fráze (viz [MerchantText]),
 * porovnávají se po celých tokenech (ne podřetězcově). 3041 klíčů.
 */
object BrandLexicon {

    /** Řádek = "klíč<TAB>categoryId". */
    val rules: List<Pair<String, String>> by lazy {
        CHUNKS.flatMap { chunk ->
            chunk.lineSequence()
                .filter { it.isNotBlank() }
                .map { line ->
                    val tab = line.indexOf('\t')
                    line.substring(0, tab) to line.substring(tab + 1)
                }
                .toList()
        }
    }

    private val CHUNKS: List<String> = listOf(
        CHUNK_0,
        CHUNK_1,
    )

    private const val CHUNK_0 = """
1 day	food_groceries
101 drogerie	shopping_drugstore
1a autoservice	transport_car_other
585 gold	shopping_other
5asec	housing_utilities
60 seconds to napoli	food_dining
7 eleven	transport_fuel
7 eleven australia	food_groceries
7 eleven canada	food_groceries
7 eleven philippines	food_groceries
99rent	transport_car_other
9round	health_sport
9round kickboxing fitness	health_sport
a lange sohne	shopping_other
a3 sport	health_sport
abarth	transport_car_other
abc polska	food_groceries
abc schuh center	shopping_clothes
abc schuhe	shopping_clothes
abele optik	health_care
abercrombie fitch	shopping_clothes
abercrombie kids	shopping_clothes
ac hotel	leisure_holidays
ace hardware	housing_home
ace tate	health_care
acura	transport_car_other
adac geschaftsstelle	leisure_holidays
adagio	leisure_holidays
adeg	food_groceries
adidas	shopping_clothes
adidas original	shopping_clothes
adidas originals	shopping_clothes
adina	leisure_holidays
adina apartment hotel	leisure_holidays
adina hotels	leisure_holidays
adler	shopping_clothes
admiral sportsbar	shopping_other
admiral sportwetten	shopping_other
admiral wettcafe	shopping_other
aeropostale	shopping_clothes
aesop	shopping_drugstore
afriquia	transport_fuel
agata meble	housing_home
agatha	shopping_other
agel	health_care
agence orange	comm_phone_internet
agencja pzu	financial_insurance
agencja ubezpieczeniowa pzu	financial_insurance
agent provocateur	shopping_clothes
agent pzu	financial_insurance
agip	transport_fuel
agip eni	transport_fuel
agnes b	shopping_clothes
aida	food_dining
aiden	leisure_holidays
aiden by best western	leisure_holidays
aiden hotel	leisure_holidays
air liquide	transport_fuel
aixam	transport_car_other
ak outlet	shopping_clothes
ak sport	shopping_clothes
aktionshaus wreesmann	shopping_other
aktivoptik	health_care
akvazoo	shopping_other
akzent hotels	leisure_holidays
alamo	transport_car_other
albert	food_groceries
albi	charity
aldi	food_groceries
aldi aldi nord group	food_groceries
aldi aldi sud group	food_groceries
aldi australia	food_groceries
aldi france	food_groceries
aldi nord	food_groceries
aldi nord deutschland	food_groceries
aldi sud	food_groceries
aldi suisse	food_groceries
aldi uk	food_groceries
aldi usa	food_groceries
aldo	shopping_clothes
aldo shoes	shopping_clothes
ale hop	charity
alexander mcqueen	shopping_clothes
alfa romeo	transport_car_other
aliexpress	shopping_other
alila	leisure_holidays
allbirds	shopping_clothes
alldrink	food_groceries
allegro	shopping_other
allegro one box	shopping_other
alliance	transport_car_other
alliance tire company	transport_car_other
allianz	financial_insurance
allianz versicherung	financial_insurance
alltours	leisure_holidays
alltoys	shopping_other
allworden	food_dining
alma gyogyszertar	health_care
alnatura	food_groceries
alnatura super natur markt	food_groceries
aloft	leisure_holidays
alpha buchhandlung	leisure_culture
alphazoo	shopping_other
alpine	transport_car_other
alpine pro	shopping_clothes
alt nbas	shopping_other
alua	leisure_holidays
alua hotels resorts	leisure_holidays
alza	shopping_electronics
alza cz	shopping_electronics
alzabox	shopping_other
amazon	shopping_other
amazon fresh	food_groceries
amazon hub	shopping_other
amazon hub locker	shopping_other
amazon locker	shopping_other
america today	shopping_clothes
american vintage	shopping_clothes
amic	transport_fuel
amic energy	transport_fuel
amorino	food_dining
amos style	shopping_clothes
amplifon	health_care
amplifon deutschland gmbh	health_care
amplifon france	health_care
ampm	food_groceries
anabel arto	shopping_clothes
anantara	leisure_holidays
andaz	leisure_holidays
anicura	shopping_other
anika schuh	shopping_clothes
anker	food_dining
ansons	shopping_clothes
antea bestattungen	others
antik smartway	health_sport
anytime fitness	health_sport
anytime fitness australia	health_sport
api ip	transport_fuel
apm monaco	shopping_other
apollo optik	health_care
apollo tyres	transport_car_other
aposto	food_dining
appkomat inpost	shopping_other
apple store	shopping_electronics
applebees	food_dining
applebees bar and grill	food_dining
applebees grill and bar	food_dining
applebees neighborhood grill bar	food_dining
apteka dbam o zdrowie	health_care
apteka gemini	health_care
apteka rodzinna	health_care
apteka s oneczna	health_care
ara schuhe	shopping_clothes
ara shoes	shopping_clothes
arabica	food_dining
arag	financial_insurance
aral	transport_fuel
aral pulse	transport_fuel
aral superwash	transport_car_other
aral tankstelle	transport_fuel
arbeiter samariter bund	shopping_other
arbeiter samariter bund deutschland	shopping_other
arbeiterwohlfahrt	shopping_other
arboria bike	health_sport
arhelan	food_groceries
aristocrazy	shopping_other
arko	food_dining
arlt computer	shopping_electronics
armani exchange	shopping_clothes
armbruster	food_dining
arnika	health_care
artotel	leisure_holidays
ascend hotel collection	leisure_holidays
asiahung	food_dining
asian paints	housing_home
asics	shopping_clothes
asko nabytek	housing_home
aston martin	transport_car_other
athletes world	shopping_clothes
aubade	shopping_clothes
auchan	food_groceries
audemars piguet	shopping_other
audi	transport_car_other
audika	health_care
auer	food_dining
auntie annes	food_dining
auntie annes pretzels	food_dining
auto teile unger	transport_car_other
autofit	transport_car_other
autograph collection	leisure_holidays
autogrill	food_dining
automat przesy kowy aliexpress	shopping_other
avec	food_groceries
avia	transport_fuel
avia xpress	transport_fuel
avis	transport_car_other
aviva	financial_insurance
avon	shopping_drugstore
aw lab	shopping_clothes
awg modecenter	shopping_clothes
awiteks	food_dining
ay yildiz	comm_phone_internet
ayk sonnenstudio	health_sport
b b hotel	leisure_holidays
b b hotels	leisure_holidays
baby club	shopping_other
baby walz	shopping_other
babyone	shopping_other
bachmeier	food_dining
back factory	food_dining
backer gortz	food_dining
backer happ	food_dining
backer steiskal	food_dining
backerei brothaus	food_dining
backerei drei ig	food_dining
backerei emil reimann	food_dining
backerei fuchs	food_dining
backerei gilgens	food_dining
backerei grimminger	food_dining
backerei happ	food_dining
backerei hosselmann	food_dining
backerei ihle	food_dining
backerei junge	food_dining
backerei kamps	food_dining
backerei keim	food_dining
backerei leifert	food_dining
backerei merzenich	food_dining
backerei nahrstedt	food_dining
backerei pappert	food_dining
backerei reimann	food_dining
backerei sander	food_dining
backerei stinges	food_dining
backerei wienerroither	food_dining
backerei wienerroither gmbh	food_dining
backhaus nahrstedt	food_dining
backstube wunsche	food_dining
backwerk	food_dining
bafra kebab	food_dining
bageterie boulevard	food_dining
bagietka	food_dining
baguette	food_dining
bahlsen	food_dining
bahlsen outlet	food_dining
bahn bkk	financial_insurance
bakmaz	food_groceries
balenciaga	shopping_clothes
baleno	shopping_clothes
balikobox	shopping_other
balikovo box	shopping_other
bally	shopping_clothes
baloise	financial_insurance
bambule	shopping_other
banana republic	shopping_clothes
banana republic factory	shopping_clothes
banco casino	shopping_other
bandi	shopping_clothes
bang olufsen	shopping_electronics
banquet	housing_home
banyan tree	leisure_holidays
barbarossa	food_dining
barbarossa backerei	food_dining
barbarossa brotkultur	food_dining
barbour	shopping_clothes
barcelo	leisure_holidays
barcelo hotels resorts	leisure_holidays
baren treff	food_dining
barenland	food_dining
barmenia	financial_insurance
barmenia versicherung	financial_insurance
barmenia versicherungen	financial_insurance
barmer	financial_insurance
barmer ersatzkasse	financial_insurance
barmer gek	financial_insurance
barrys	health_sport
baskin robbins	food_dining
bata	shopping_clothes
bath body works	shopping_drugstore
bauhaus	housing_home
bauking	housing_home
baumax	housing_home
baume mercier	shopping_other
bauspezi	housing_home
baywa	housing_home
bb q chicken	food_dining
bears friends	food_dining
beat81	health_sport
becker floge	health_care
bed breakfast hotels	leisure_holidays
bee charging solutions	transport_fuel
beets roots	food_dining
beko	shopping_electronics
bellaflora	housing_home
belmond	leisure_holidays
belushis	food_dining
ben jerrys	food_dining
bencinski servis petrol	transport_fuel
bene	housing_home
bens cookies	food_dining
bentley	transport_car_other
benu	health_care
benu apotheek	health_care
benu aptieka	health_care
benu vaistine	health_care
benz wein und getrankemarkt	food_groceries
bepon	shopping_clothes
berlin doner kebap	food_dining
berlitz	leisure_education
bershka	shopping_clothes
best hotels	leisure_holidays
best western	leisure_holidays
best western hotels resorts	leisure_holidays
best western plus	leisure_holidays
best western premier	leisure_holidays
bestdrive	transport_car_other
betty barclay	shopping_clothes
beyfin	transport_fuel
bicikelj	health_sport
bicy	health_sport
biedronka	food_groceries
biesiadowo	food_dining
big direkt gesund	financial_insurance
big gesundheit	financial_insurance
big star	shopping_clothes
bigmat	housing_home
bijou brigitte	shopping_other
bike energy	transport_fuel
bike energy ladestation	transport_fuel
bike gallery	health_sport
bikekia	health_sport
bikeu	health_sport
bilgro	food_groceries
billa	food_groceries
billa cesko	food_groceries
billa marktkuche	food_dining
billa now	food_groceries
billa plus	food_groceries
billa stop shop	food_groceries
billa unterwegs	food_groceries
billabong	shopping_clothes
bimba lola	shopping_clothes
bimba y lola	shopping_clothes
binder optik	health_care
bio company	food_groceries
biotechusa	shopping_drugstore
bipa	shopping_drugstore
birkenstock	shopping_clothes
bkk axel springer	financial_insurance
bkk beiersdorf ag	financial_insurance
bkk gesundheit	financial_insurance
bkk vbu	financial_insurance
black red white	housing_home
blazek	shopping_clothes
blikle	food_dining
bliska	transport_fuel
block house	food_dining
blue tomato	shopping_clothes
blume 2000	housing_home
blumen b b	housing_home
blumen risse	housing_home
bobbi brown	shopping_drugstore
boconcept	housing_home
body attack	shopping_drugstore
body glove	shopping_clothes
bodystreet	health_sport
bodzio	housing_home
boesner	leisure_culture
boggi milano	shopping_clothes
bogner	shopping_clothes
bolia	housing_home
bonita	shopping_clothes
bonita men	shopping_clothes
bonito	leisure_culture
bonjour	food_groceries
borek	food_dining
bosch	shopping_electronics
bosch car service	transport_car_other
bosch ebike systems	transport_fuel
bosch service	transport_car_other
bosco	shopping_clothes
bose	shopping_electronics
boss mobel	housing_home
bottega veneta	shopping_clothes
box now	shopping_other
box now hrvatska	shopping_other
bp america	transport_fuel
bp connect	transport_fuel
bp gas station	transport_fuel
brandy melville	shopping_clothes
braun mobel	housing_home
braun mobel center	housing_home
brax	shopping_clothes
breitling	shopping_other
breno	housing_home
breuninger	shopping_clothes
brewdog	food_dining
brezelbackerei ditsch	food_dining
brezelkonig	food_dining
bricoman	housing_home
bricomarche	housing_home
bridgestone	transport_car_other
bridgestone india	transport_car_other
brillen de	health_care
brillen rottler	health_care
brillux	housing_home
brioche doree	food_dining
brloh	shopping_electronics
brnenka	food_groceries
brooks brothers	shopping_clothes
brothaus	food_dining
brothaus cafe	food_dining
brunello cucinelli	shopping_clothes
bruno banani	shopping_clothes
bs petrol	transport_fuel
bubbles	housing_utilities
bubi	health_sport
buccellati	shopping_other
buchbinder	transport_car_other
bucher pustet	leisure_culture
buchhandlung konig	leisure_culture
buchhandlung walther konig	leisure_culture
budget car rental	transport_car_other
budmil	shopping_clothes
budni	shopping_drugstore
budnikowsky	shopping_drugstore
bugatti	transport_car_other
bugatti fashion	shopping_clothes
build your dreams	transport_car_other
bulgari	shopping_clothes
bulthaup	housing_home
bundesinnungskrankenkasse	financial_insurance
bundesinnungskrankenkasse gesundheit	financial_insurance
burberry	shopping_clothes
burger king	food_dining
burger king brasil	food_dining
burger king espana	food_dining
burgerheart	food_dining
burgerme	food_dining
burgermeister	food_dining
burguer king	food_dining
burrito company	food_dining
busch	food_dining
bushman	shopping_clothes
butlers	housing_home
bvlgari	shopping_clothes
bw premier collection	leisure_holidays
bw signature collection	leisure_holidays
byd auto	transport_car_other
bydgoski rower aglomeracyjny	health_sport
bytom	shopping_clothes
c c schaper	food_groceries
cadera	food_dining
cadillac	transport_car_other
cafe amazon	food_dining
cafe bar celona	food_dining
cafe cappuccino	food_dining
cafe coffee day	food_dining
cafe coton	shopping_clothes
cafe del sol	food_dining
cafe extrablatt	food_dining
cafe frei	food_dining
cafe nero	food_dining
caffe nero	food_dining
caffe pascucci	food_dining
calida	shopping_clothes
call a bike	health_sport
call a pizza	food_dining
call it spring	shopping_clothes
calliope	shopping_clothes
caltex	food_groceries
calvin klein	shopping_clothes
calvin klein jeans	shopping_clothes
calypso	health_sport
calypso fitness club	health_sport
calzedonia	shopping_clothes
camaieu	shopping_clothes
camel active	shopping_clothes
camp david	shopping_clothes
campanile	leisure_holidays
camper	shopping_clothes
campsa	transport_fuel
campus suite	food_dining
canada goose	shopping_clothes
canali	shopping_clothes
canopy	leisure_holidays
cap markt	food_groceries
capi	shopping_electronics
capi electronics	shopping_electronics
cappuccino	food_dining
carglass	transport_car_other
carhartt work in progress	shopping_clothes
carlings	shopping_clothes
carls jr	food_dining
carrefour	food_groceries
carrefour express	food_groceries
carrefour market	transport_fuel
carters	shopping_clothes
carters babies and kids	shopping_clothes
cartier	shopping_other
caseys	transport_fuel
caseys general store	transport_fuel
castorama	housing_home
catalonia	leisure_holidays
catalonia hotels resorts	leisure_holidays
catimini	shopping_clothes
cba	food_groceries
cecil	shopping_clothes
cedok	leisure_holidays
cef rm 36 6	health_care
celine	shopping_clothes
celio	shopping_clothes
centershop	shopping_other
centre commercial e leclerc	food_groceries
centro hotel	leisure_holidays
centro hotels	leisure_holidays
centrum zdrowia	health_care
ceska podnikatelska pojistovna	financial_insurance
champion	shopping_clothes
chanel	shopping_clothes
change lingerie	shopping_clothes
chargepoint	transport_fuel
charles vogele	shopping_clothes
chata polska	food_groceries
chateau dax	housing_home
chatime	food_dining
chaumet	shopping_other
checkers	food_dining
cheesecake corner	food_dining
chery	transport_car_other
chevrolet	transport_car_other
chevron	transport_fuel
chicco	shopping_other
chilis	food_dining
china wok	food_dining
chipotle	food_dining
chloe	shopping_clothes
chorten	food_groceries
christ	shopping_other
chromek	health_sport
chrysler	transport_car_other
cigkoftem	food_dining
cigo	leisure_culture
cigo deutschland	leisure_culture
cinema city	leisure_culture
cinemark	leisure_culture
cinemax	leisure_culture
cinemaxx	leisure_culture
cinemaxx europe	leisure_culture
cineplex	leisure_culture
cineplex deutschland	leisure_culture
cineplexx	leisure_culture
cinepolis	leisure_culture
cinestar	leisure_culture
cinnabon	food_dining
circle k	transport_fuel
circle k canada	transport_fuel
citgo	transport_fuel
citroen	transport_car_other
city express	shopping_other
city kiosk	shopping_other
ck hydrotour	leisure_holidays
ck satur	leisure_holidays
ck turancar	leisure_holidays
claires	shopping_clothes
claires accessories	shopping_clothes
claires stores	shopping_clothes
clarks	shopping_clothes
claudie pierlot	shopping_clothes
clever fit	health_sport
closed fashion label	shopping_clothes
club med	leisure_holidays
club wyndham	leisure_holidays
coccinelle	shopping_clothes
coccodrillo	shopping_clothes
coco	food_dining
coco fresh tea juice	food_dining
coffee bean tea leaf	food_dining
coffee fellows	food_dining
coffee lab	food_dining
coffee republic	food_dining
coffee time	food_dining
coffeeshop company	food_dining
cold stone	food_dining
cold stone creamery	food_dining
cole haan	shopping_clothes
coles	transport_fuel
coles express	transport_fuel
colins	shopping_clothes
colloseum	shopping_clothes
columbia	shopping_clothes
comcave	leisure_education
comcave college	leisure_education
comfort suites	leisure_holidays
comma	shopping_clothes
concordia	financial_insurance
confectionery house vatsak	food_dining
conrad	shopping_electronics
continental ag	transport_car_other
continentale	financial_insurance
continentale krankenversicherung	financial_insurance
continentale versicherung	financial_insurance
converse	shopping_clothes
coolblue	shopping_electronics
coop abc	food_groceries
coop cesko	food_groceries
coop jednota	food_groceries
coop konzum	food_groceries
coop mini	food_groceries
coop szuper	food_groceries
coop terno	food_groceries
coop tip	food_groceries
coop tuty	food_groceries
coral travel	leisure_holidays
cortez	food_groceries
cosmedica	health_care
cosmoparis	shopping_clothes
costa	food_dining
costa coffee	food_dining
costa coffee costa	food_dining
costa drive through	food_dining
costa drive thru	food_dining
costcutter	food_groceries
costcutters	food_groceries
cotelac	shopping_clothes
cotton on	shopping_clothes
cotton on kids	shopping_clothes
courtyard	leisure_holidays
courtyard marriott	leisure_holidays
coyote ugly	food_dining
coyote ugly saloon	food_dining
crocs	shopping_clothes
crodux	transport_fuel
cropp	shopping_clothes
crossfit	health_sport
crowne plaza	leisure_holidays
csob poistovna	financial_insurance
cuk ubezpieczenia	financial_insurance
cukiernia cieslikowski	food_dining
cukiernia sowa	food_dining
cupra	transport_car_other
curaprox smile shop	shopping_drugstore
curio collection	leisure_holidays
curio collection by hilton	leisure_holidays
curves	health_sport
cyberport	shopping_electronics
czas na herbate	food_dining
czeladzki rower miejski	health_sport
d and g	shopping_clothes
da grasso	food_dining
dacia	transport_car_other
daffer	shopping_other
daihatsu	transport_car_other
dairy queen	food_dining
dak gesundheit	financial_insurance
dalioil	transport_fuel
dallmeyers backhus	food_dining
dan john	shopping_clothes
danisches bettenlager	housing_home
darner	transport_car_other
darwina pl	food_groceries
das futterhaus	shopping_other
das macht sinn	shopping_clothes
dat backhus	food_dining
datacomp	shopping_electronics
datart	shopping_electronics
dates mobile	comm_phone_internet
db reisezentrum	leisure_culture
db servicestore	shopping_other
dbam o zdrowie	health_care
dealz	shopping_other
dean david	food_dining
debeka	financial_insurance
debeukelaer	food_dining
debeukelaer factory outlet	food_dining
decathlon	health_sport
decimas	shopping_clothes
decodom	housing_home
dedalus	leisure_culture
defacto	shopping_clothes
dehner	housing_home
deichmann	shopping_clothes
deichmann schuhe	shopping_clothes
deinfach	shopping_other
dek stavebniny	housing_home
dekra	transport_car_other
delifrance	food_dining
delikana	food_dining
delikateso	charity
delikatesy centrum	food_groceries
delikatesy premium	food_groceries
delikatesy sezam	food_groceries
delikatesy slawex	food_groceries
delta hotels	leisure_holidays
denns biomarkt	food_groceries
dennys	food_dining
dentix	health_care
der backer ruetz	food_dining
der deutsches reiseburo	leisure_holidays
der mann	food_dining
der reiseburo	leisure_holidays
dermacol	shopping_drugstore
derpart reiseburo	leisure_holidays
dertour	leisure_holidays
dertour reiseburo	leisure_holidays
design hotels	leisure_holidays
design offices	work_tools
desigual	shopping_clothes
despar	food_groceries
destination by hyatt	leisure_holidays
deutsche bahn	leisure_culture
deutsche bkk	financial_insurance
deutsche post	shopping_other
deutsche post ag	shopping_other
deutsche see	food_groceries
deutsche telekom	comm_phone_internet
deutscher kinderschutzbund	shopping_other
deutsches jugendherbergswerk	leisure_holidays
deutsches rotes kreuz	health_care
devialet	shopping_electronics
devk	financial_insurance
dhl box 24 7	shopping_other
dhl csomagautomata	shopping_other
dhl packstation	shopping_other
dhl paketbox	shopping_other
dhl paketshop	shopping_other
dhl pop box	shopping_other
dhl poststation	shopping_other
dhl vertrieb	shopping_other
dia market	food_groceries
diagnostyka	health_care
diagnostyka laboratorium	health_care
diakonie	shopping_other
diamond resorts	leisure_holidays
die continentale	financial_insurance
die lohners	food_dining
die techniker	financial_insurance
diego	housing_home
digicel	comm_phone_internet
dille kamille	housing_home
ding tea	food_dining
dinh van	shopping_other
dinh van paris	shopping_other
dino	food_groceries
diona	food_groceries
dior	shopping_clothes
diptyque	shopping_drugstore
diska	food_groceries
ditsch	food_dining
diverse	shopping_clothes
djak	shopping_clothes
djak sport	shopping_clothes
dkny	shopping_clothes
dm drogerie markt	shopping_drugstore
dm drogerie markt deutschland	shopping_drugstore
dnipro m	housing_home
dobbe	food_dining
doc marten	shopping_clothes
doc martens	shopping_clothes
doctor marten	shopping_clothes
doctor martens	shopping_clothes
dodge	transport_car_other
dohanybolt	food_dining
dolce and gabana	shopping_clothes
dolce and gabanna	shopping_clothes
dolce and gabbanna	shopping_clothes
dolce gabbana	shopping_clothes
dolce y gabana	shopping_clothes
dolce y gabanna	shopping_clothes
dolce y gabbana	shopping_clothes
dolce y gabbanna	shopping_clothes
dolcezza	shopping_clothes
dom lekow	health_care
dominos	food_dining
dominos australia	food_dining
dominos pizza	food_dining
dominos pizza india	food_dining
dominos pizza nsr	food_dining
donkey republic	health_sport
donna karan	shopping_clothes
donna karan new york	shopping_clothes
dorint	leisure_holidays
dornseifer	food_groceries
dornseifers frischemarkt	food_groceries
dorotheum juwelier	shopping_other
doubletree	leisure_holidays
douglas	shopping_drugstore
dovera	financial_insurance
dovera zdravotna poistovna	financial_insurance
dpd dynamic parcel distribution	shopping_other
dpd paketshop	shopping_other
dpd pickup station	shopping_other
dr martens	shopping_clothes
dr max	health_care
dr max box	shopping_other
dracik	shopping_other
drei ig	food_dining
dress barn	shopping_clothes
dressmann	shopping_clothes
driver center	transport_car_other
drogeria natura	shopping_drugstore
drogerie polskie	shopping_drugstore
drogeriemarkt muller	shopping_drugstore
ds automobiles	transport_car_other
du pareil au meme	shopping_clothes
ducati	transport_car_other
dufry	shopping_other
duka	housing_home
dulux	housing_home
dulux paints	housing_home
dune london	shopping_clothes
dunhill	shopping_clothes
dunkin	food_dining
dunkin doughnuts	food_dining
dunlop	transport_car_other
dursty	food_groceries
duzy ben	food_groceries
e center	food_groceries
e dym	food_dining
e leclerc	food_groceries
e neukauf	food_groceries
e on danmark	transport_fuel
e on drive	transport_fuel
e wald	transport_fuel
e wald gmbh	transport_fuel
e wald ladestation	transport_fuel
easyapotheke	health_care
easybox	shopping_other
easyfitness	health_sport
easyhotel	leisure_holidays
eat happy	food_dining
eataly	food_groceries
eb games	shopping_electronics
ebl naturkost	food_groceries
ecco	shopping_clothes
ecco shoes	shopping_clothes
eco express	housing_utilities
edeka	food_groceries
edeka aktiv markt	food_groceries
edeka foodservice	food_groceries
edeka getrankemarkt	food_groceries
edeka neukauf	food_groceries
edeka xpress	food_groceries
eemobility	transport_fuel
eemobility gmbh	transport_fuel
ehrhardt reifen autoservice	transport_car_other
ehrle	transport_car_other
eilles	food_dining
einstein cafe	food_dining
einstein kaffee	food_dining
eko montenegro	transport_fuel
el n london	food_dining
elan	transport_fuel
elbenwald	charity
electronicpartner	shopping_electronics
electronics boutique eb games	shopping_electronics
elena miro	shopping_clothes
elisabetta franchi	shopping_clothes
elli	food_groceries
elli markt	food_groceries
emil reimann	food_dining
emilio adani	shopping_clothes
empik	leisure_culture
emporio armani	shopping_clothes
enchilada	food_dining
endlich ohne	health_sport
endlich ohne tattooentfernung	health_sport
engbers	shopping_clothes
eni shop	food_groceries
eni station	transport_fuel
eni wash	transport_car_other
enterprise	transport_car_other
enterprise car rental	transport_car_other
enza home	housing_home
equiva	health_sport
equivalenza	shopping_drugstore
ergo	financial_insurance
ergo direkt	financial_insurance
ergo direktversicherung	financial_insurance
ergo hestia	financial_insurance
ergo versicherung	financial_insurance
eric bompard	shopping_clothes
ermenegildo zegna	shopping_clothes
ernstings family	shopping_clothes
erotic store venus	shopping_other
erster wiener	food_dining
es teler 77	food_dining
esanelle	health_sport
escada	shopping_clothes
eskulap	health_care
esotiq	shopping_clothes
espresso house	food_dining
esprit	shopping_clothes
essanelle	health_sport
essanelle ihr friseur	health_sport
essentiel antwerp	shopping_clothes
esso	transport_fuel
esso canada	transport_fuel
esso express	transport_fuel
esso service station	transport_fuel
esso snack shop	food_groceries
esso tigerwash	transport_car_other
etam	shopping_clothes
etam lingerie	shopping_clothes
eterna	shopping_clothes
etro	shopping_clothes
etsan	food_groceries
euro sklep	food_groceries
euromaster	transport_car_other
euronics	shopping_electronics
euronics centre	shopping_electronics
euronics deutschland	shopping_electronics
eurooil	transport_fuel
eurooil cesko	transport_fuel
europcar	transport_car_other
euroshop	shopping_other
eurospar	food_groceries
eurospin	food_groceries
eurospin italia	food_groceries
eurostars	leisure_holidays
eurostars hotel company	leisure_holidays
eurostars hotels	leisure_holidays
everfuel	transport_fuel
evergreen international hotels	leisure_holidays
exe hotels	leisure_holidays
executive residency	leisure_holidays
exisport	health_sport
expert deutschland	shopping_electronics
express one	shopping_other
express one csomagpont	shopping_other
express one magyarorszag	shopping_other
express one montenegro	shopping_other
express one slovensko	shopping_other
extrawurst	food_dining
extreme intimo	shopping_clothes
extreme pizza	food_dining
exxon	food_groceries
exxon tiger mart	food_groceries
eyes more	health_care
faberlic	shopping_drugstore
fahrrad xxl	health_sport
fairmont	leisure_holidays
fajn zoo	shopping_other
fakta	food_groceries
falconeri	shopping_clothes
falke	shopping_clothes
famila	food_groceries
familienbackerei grimminger	food_dining
familijna	food_dining
fann	shopping_drugstore
farmacia	health_care
farmacia hrvatska	health_care
farmfoods	food_groceries
farrow ball	housing_home
fastned	transport_fuel
fastrack	shopping_other
faxcopy	shopping_other
federal express	shopping_other
fedex	shopping_other
fedex office	shopping_other
fedex ship center	shopping_other
felbermayr	housing_home
fendi	shopping_clothes
feneberg	food_groceries
ferrari	transport_car_other
fiat	transport_car_other
fiat professional	transport_car_other
fielmann	health_care
figaros pizza	food_dining
fila	shopping_clothes
fireaway	food_dining
fireaway pizza	food_dining
first day	food_groceries
first reiseburo	leisure_holidays
first stop	transport_car_other
fisker	transport_car_other
fisker inc	transport_car_other
fissler	housing_home
fitbox	health_sport
fitinn	health_sport
fitness first	health_sport
fitness first ladies	health_sport
fitseveneleven	health_sport
fitshop	health_sport
fitx	health_sport
five guys	food_dining
five oclock	food_dining
flair hotel	leisure_holidays
flair hotels	leisure_holidays
flair hotels ev	leisure_holidays
flamengo	housing_home
fleischerei richter	food_groceries
flight centre	leisure_holidays
flotte	health_sport
flying tiger copenhagen	shopping_other
fogo de chao	food_dining
fokus	health_care
fokus optik	health_care
food basics	food_groceries
foot locker	shopping_clothes
foot locker europe	shopping_clothes
foot solutions	shopping_clothes
ford	transport_car_other
ford motor company	transport_car_other
forever 21	shopping_clothes
fornetti	food_dining
forstinger	transport_car_other
fortuna	shopping_other
fossil	shopping_other
four points by sheraton	leisure_holidays
four seasons	leisure_holidays
foxpost	shopping_other
frac	food_groceries
franck provost	health_sport
franky getrankemarkt	food_groceries
freddy fresh	food_dining
freie tankstelle	transport_fuel
french connection	shopping_clothes
fresh corner	food_groceries
fresh corner restaurant	food_dining
fresh plus	food_groceries
freshmarket	food_groceries
fressnapf	shopping_other
fressnapf deutschland	shopping_other
friseur klier	health_sport
frisor klier	health_sport
fristo	food_groceries
fristo getrankemarkt	food_groceries
frittenwerk	food_dining
fritz berger	health_sport
fuchs backerei	food_dining
fusakle	shopping_clothes
fusalp	shopping_clothes
fussl	shopping_clothes
futterhaus	shopping_other
g star	shopping_clothes
g star raw	shopping_clothes
gabor	shopping_clothes
gaggenau	housing_home
gaik	food_groceries
galeria kaufhof	shopping_other
galeria kaufhof gmbh	shopping_other
galeria wypiekow	food_dining
galeria wypiekow lubaszka	food_dining
galeries lafayette	shopping_other
galerija podova	housing_home
gama	food_groceries
games workshop	leisure_culture
gamestop	shopping_electronics
gant	shopping_clothes
gap body	shopping_clothes
gap kids	shopping_clothes
garage renault	transport_car_other
gatta	shopping_clothes
gautier	housing_home
gavranovic	food_groceries
geco	food_dining
geco tabak	leisure_culture
geely	transport_car_other
geers	health_care
geers horakustik	health_care
geers horgerate	health_care
gemini	health_care
generali	financial_insurance
generali espana	financial_insurance
genesis	transport_car_other
genesis motor	transport_car_other
genesis motors	transport_car_other
genol	transport_fuel
georg jos kaes	food_groceries
geovelo opole	health_sport
geox	shopping_clothes
german youth hostel association	leisure_holidays
gerry weber	shopping_clothes
gesellschaft fur technische uberwachung	transport_car_other
getranke arena	food_groceries
getranke city	food_groceries
getranke hoffmann	food_groceries
getranke quelle	food_groceries
getrankeland	food_groceries
getrankewelt	food_groceries
gg tabak	leisure_culture
gigasport	health_sport
gilgens	food_dining
gilgens backerei	food_dining
gina laura	shopping_clothes
giordano	shopping_clothes
giorgio armani	shopping_clothes
givenchy	shopping_clothes
glo studio	food_dining
globetrotter	health_sport
globi	food_groceries
globus	food_groceries
globus baumarkt	housing_home
glocken backerei	food_dining
gloria jeans	food_dining
gls balikomat	shopping_other
gls paketomat	shopping_other
gls parcel locker	shopping_other
gmunder ersatzkasse	financial_insurance
gnc live well	shopping_drugstore
go asia	food_groceries
goeken backen	food_dining
golden goose	shopping_clothes
golden tulip	leisure_holidays
good lood	food_dining
goraco polecam	food_dining
gortz	food_dining
gortz 17	shopping_clothes
gosch sylt	food_dining
gothaer	financial_insurance
gr nn kontakt	transport_fuel
gradska ljekarna zagreb	health_care
grand hyatt	leisure_holidays
grandoptical	health_care
granit	housing_home
grawe	financial_insurance
grazer wechselseitige	financial_insurance
grazer wechselseitige versicherung ag	financial_insurance
great wall	transport_car_other
great wall motors	transport_car_other
green cafe nero	food_dining
green caffe nero	food_dining
green motion	transport_car_other
green motion car rental	transport_car_other
greenpoint	shopping_clothes
greet	leisure_holidays
grene	housing_home
grieneisen bestattungen	others
grimminger	food_dining
gro hamburger bestattungsinstitut	others
grochola	food_dining
grom	food_dining
grom gelaterie artigianali	food_dining
grom gelato	food_dining
gromulski	food_dining
groszek	food_groceries
groupama	financial_insurance
grycan	food_dining
grzybki	food_dining
gucci	shopping_clothes
gudrun sjoden	shopping_clothes
guess	shopping_clothes
guess accessories	shopping_clothes
guidepost montessori	shopping_other
guinot	health_sport
gulf	transport_fuel
gut gebucht	leisure_holidays
gutkauf	food_groceries
gwm ora	transport_car_other
gyorbike	health_sport
gzella	food_groceries
h m home	housing_home
h m kids	shopping_clothes
h10 hotels	leisure_holidays
haagen dazs	food_dining
habitat	housing_home
habitat non uk	housing_home
hackett	shopping_clothes
hackett london	shopping_clothes
hada	shopping_clothes
hagebau	housing_home
hagebaumarkt	housing_home
haidilao hot pot	food_dining
hair express	health_sport
haircut express	health_sport
hairkiller	health_sport
halfprice	shopping_clothes
hallhuber	shopping_clothes
hallo pizza	food_dining
hamburg munchener krankenkasse	financial_insurance
hampton	leisure_holidays
handelshof	food_groceries
handelskrankenkasse	financial_insurance
hanesbrands	shopping_clothes
hans im gluck	food_dining
hansaton	health_care
hansemerkur	financial_insurance
happ	food_dining
happy lemon	food_dining
happy pets	shopping_other
happy socks	shopping_clothes
hard rock cafe	food_dining
hard rock hotel	leisure_holidays
hardeck	housing_home
hardees	food_dining
harley davidson	transport_car_other
harman kardon	shopping_electronics
harmont blaine	shopping_clothes
hartlauer	shopping_electronics
haubis	food_dining
haus des doners	food_dining
havaianas	shopping_clothes
haval	transport_car_other
havlikova apoteka	shopping_drugstore
hdi direkt	financial_insurance
hdi gerling	financial_insurance
hdi global	financial_insurance
hebe	shopping_drugstore
heberer	food_dining
hecht	housing_home
heinrich von allworden	food_dining
helbing	food_dining
helios	leisure_culture
hellweg	housing_home
helly hansen	shopping_clothes
helvetia	financial_insurance
hema	shopping_other
hema b v	shopping_other
henry laden	charity
herkules bau garten	housing_home
herkules ecenter	food_groceries
herman miller	housing_home
hermes	shopping_clothes
hert	food_dining
hertz	transport_car_other
hertz car rental	transport_car_other
hertz rent a car	transport_car_other
hervis	health_sport
hervis sport	health_sport
hervis sports	health_sport
herzog brauer	shopping_clothes
hesburger	food_dining
high5	health_sport
hilton	leisure_holidays
hilton garden inn	leisure_holidays
hilton grand vacations	leisure_holidays
hilton hotels resorts	leisure_holidays
hinnerbacker	food_dining
hino motors	transport_car_other
hit ullrich	food_groceries
hkk krankenkasse	financial_insurance
hofer	food_groceries
hofer diskont	transport_fuel
hofer osterreich	food_groceries
hoffner	housing_home
hofpfisterei	food_dining
hoka	health_sport
holab	food_groceries
holiday inn	leisure_holidays
holiday inn express	leisure_holidays
holiday inn express suites	leisure_holidays
holland blumen	housing_home
holland blumen mark	housing_home
hollister	shopping_clothes
holmes place	health_sport
holzleitner	shopping_electronics
home you	housing_home
home24	housing_home
honda	transport_car_other
honda cars	transport_car_other
hooters	food_dining
horgerate seifert	health_care
horl	food_groceries
hornbach	housing_home
hosselmann	food_dining
hotel ibis	leisure_holidays
hotel indigo	leisure_holidays
hoteles meininger	leisure_holidays
house of hoops	shopping_clothes
howard johnson	leisure_holidays
hoyer	transport_fuel
hruska	food_groceries
hti gruppe	housing_home
huawei	comm_phone_internet
huber shop	shopping_clothes
hugendubel	leisure_culture
hugo boss	shopping_clothes
huk coburg	financial_insurance
humana	shopping_clothes
humana people to people	shopping_clothes
humanic	shopping_clothes
hunkemoller	shopping_clothes
hush puppies	shopping_clothes
husky	health_sport
husqvarna	housing_home
hussel	food_dining
hussel confiserie	food_dining
hyatt	leisure_holidays
hyatt centric	leisure_holidays
hyatt house	leisure_holidays
hyatt place	leisure_holidays
hyatt regency	leisure_holidays
hydrotour	leisure_holidays
hyeondaegia	transport_car_other
hyundai	transport_car_other
hyundai india	transport_car_other
iberostar	leisure_holidays
iberostar hotels resorts	leisure_holidays
ibis	leisure_holidays
ibis budget	leisure_holidays
ibis hotel	leisure_holidays
ibis styles	leisure_holidays
iceland	food_groceries
iceland foods	food_groceries
idea super	food_groceries
ifly	health_sport
ihle	food_dining
ihle backerei	food_dining
ihop	food_dining
ihr landbacker	food_dining
iittala	housing_home
ikea	housing_home
ikea bistro	food_dining
ikea cafe	food_dining
ikea csomagpont	shopping_other
ikea planning studio	housing_home
ikea planovaci studio	housing_home
ikea restaurant	food_dining
ikea swedish food market	food_groceries
ikk bremen bremerhaven	financial_insurance
ikk classic	financial_insurance
ikk die innovationskasse	financial_insurance
ikk gesund plus	financial_insurance
ikk hamburg	financial_insurance
ikk mecklenburg vorpommern	financial_insurance
ikk nord	financial_insurance
ikk sachsen	financial_insurance
ikk sachsen anhalt	financial_insurance
ikk schleswig holstein	financial_insurance
ikk thuringen	financial_insurance
imd berlin	health_care
immergrun	food_dining
immergun	food_dining
imo car wash	transport_car_other
imo car wash arc	transport_car_other
impact hub	work_tools
indian accessories brand	shopping_other
indian motorcycle	transport_car_other
indian watch co	shopping_other
indian watch company	shopping_other
infiniti	transport_car_other
inglot	shopping_drugstore
injoy	health_sport
injoy fitness	health_sport
inmedio	leisure_culture
inmotion	shopping_electronics
innogy	transport_fuel
innogy emobility solutions gmbh	transport_fuel
innogy se	transport_fuel
innside by melia	leisure_holidays
innside hotel	leisure_holidays
innungskrankenkasse nord	financial_insurance
inovine	shopping_other
instabox	shopping_other
inter cars	transport_car_other
intercityhotel	leisure_holidays
intercontinental	leisure_holidays
intercontinental hotels resorts	leisure_holidays
intermarche	food_groceries
intermarche contact	transport_fuel
intermarche super	food_groceries
international house of pancakes	food_dining
international watch co	shopping_other
international watch company	shopping_other
interspar	food_groceries
interspar restaurant	food_dining
intersport	health_sport
intersport deutschland	health_sport
intertoys	shopping_other
intimissimi	shopping_clothes
intimissimi uomo	shopping_clothes
intratuin	housing_home
invia	leisure_holidays
inxpress	shopping_other
ionity	transport_fuel
ionity gmbh	transport_fuel
iqos	food_dining
istores	shopping_electronics
istyle	shopping_electronics
isuzu	transport_car_other
itaka	leisure_holidays
itstyle	shopping_drugstore
ixina	housing_home
izod	shopping_clothes
j m weston	shopping_clothes
jacadi	shopping_clothes
jack jones	shopping_clothes
jack wolfskin	health_sport
jacks	food_groceries
jacques wein depot	food_groceries
jaeger lecoultre	shopping_other
jaguar	transport_car_other
jannys	food_dining
jannys eis	food_dining
jasmil	shopping_clothes
jawa	shopping_drugstore
jawoll	shopping_other
jd sports	health_sport
jdv by hyatt	leisure_holidays
jean louis david	health_sport
jeans fritz	shopping_clothes
jednota	food_groceries
jeep	transport_car_other
jil sander	shopping_clothes
jimmy choo	shopping_clothes
jo joe	leisure_holidays
jo malone	shopping_drugstore
joe the juice	food_dining
john reed fitness	health_sport
johnny rocket	food_dining
johnny rockets	food_dining
johnson fitness wellness	health_sport
johnson fitness wellness store	health_sport
jokers	leisure_culture
jollibee	food_dining
jotun	housing_home
juan valdez cafe	food_dining
jula	housing_home
julius meinl	food_dining
junge	food_dining
junge backerei	food_dining
junge die backerei	food_dining
junited autoglas	transport_car_other
jurki	transport_fuel
jurlique	shopping_drugstore
jurys	leisure_holidays
jurys inn	leisure_holidays
just over the top	shopping_clothes
juwelier kraemer	shopping_other
jw marriott	leisure_holidays
jw marriott hotels	leisure_holidays
jysk	housing_home
jysk romania	housing_home
k k schuh center	shopping_clothes
k kiosk	leisure_culture
k m elektronik	shopping_electronics
k u backerei	food_dining
kaes	shopping_clothes
kafeteria	food_dining
kaisers	food_dining
kaisers backstube	food_dining
kaisers gute backstube	food_dining
kakadu zoo	shopping_other
kakto	shopping_electronics
kalyan jewellers	shopping_other
kamera express	leisure_culture
kamps	food_dining
kanizsabike	health_sport
kanzelsberger	leisure_culture
kappahl	shopping_clothes
karcher	transport_car_other
karcher clean park	transport_car_other
karl lagerfeld	shopping_clothes
karstadt	shopping_other
karstadt kaufhof	shopping_other
karstadt warenhaus	shopping_other
karstadt warenhaus gmbh	shopping_other
kartell	housing_home
kate spade new york	shopping_clothes
kate spade ny	shopping_clothes
kaufhaus stolz	shopping_other
kaufland	food_groceries
kaufmannische krankenkasse	financial_insurance
kaufmannische krankenkasse kkh	financial_insurance
kawasaki	transport_car_other
kbtr	food_dining
kebab king	food_dining
keim	food_dining
keim gmbh	food_dining
kempinski	leisure_holidays
kenzo	shopping_clothes
kf tea	food_dining
kfc	food_dining
kfh nierenzentrum	health_care
kia motors	transport_car_other
kiabi	shopping_clothes
kids foot locker	shopping_clothes
kiehls	shopping_drugstore
kiehls since 1851	shopping_drugstore
kielecki rower miejski	health_sport
kieser training	health_sport
kig l	shopping_clothes
kiko	shopping_drugstore
kiko milano	shopping_drugstore
kimpton	leisure_holidays
kind horgerate	health_care
kinderschutzbund	shopping_other
kinekus	shopping_other
king kebab	food_dining
kino za rogiem	leisure_culture
klas	food_groceries
klas potraviny	food_groceries
kleider bauer	shopping_clothes
kleins backstube	food_dining
klier	health_sport
klipp	health_sport
km prona	transport_fuel
knappschaft	financial_insurance
koala tours	leisure_holidays
koberce trend	housing_home
kochloffel	food_dining
kodak	leisure_culture
kodak express	leisure_culture
kodi	housing_home
koenigsegg	transport_car_other
koh i noor	shopping_other
kohler	housing_home
kolektura	shopping_other
kolesce	health_sport
kolle zoo	shopping_other
kolls	food_dining
kolls familienbackerei	food_dining
kolporter	leisure_culture
komfort	housing_home
komputronik	shopping_electronics
komunalna poistovna	financial_insurance
konig	leisure_culture
konzum	food_groceries
konzum balkans	food_groceries
konzum cesko	food_groceries
kookai	shopping_clothes
kooperativa	financial_insurance
korperformen	health_sport
koton	shopping_clothes
kralupol	transport_fuel
kramer pferdesport	health_sport
krass optik	health_care
krispy kreme	food_dining
krispy kreme doughnuts	food_dining
kropka	food_groceries
krotower	health_sport
krowarzywa	food_dining
krskolesom	health_sport
krumet	shopping_other
kubenz	shopping_clothes
kuchen aktuell	housing_home
kuchen schaffrath	housing_home
kuchentreff	housing_home
kult	shopping_clothes
kung fu tea	food_dining
kuoni	leisure_holidays
kusmi tea	food_dining
kvety victor	housing_home
kyriad	leisure_holidays
la curacao	shopping_electronics
la martina	shopping_clothes
la vie en rose	shopping_clothes
laboo	shopping_drugstore
lacolada	housing_utilities
lacoste	shopping_clothes
lada	transport_car_other
lagerbox	housing_utilities
lagerhaus	housing_home
lagerhaus osterreich	housing_home
lamborghini	transport_car_other
lancia	transport_car_other
land rover	transport_car_other
landbackerei ihle	food_dining
landbackerei stinges	food_dining
langham hotels resorts	leisure_holidays
lara bags	shopping_clothes
lavazza	food_dining
lawson	food_groceries
lc waikiki	shopping_clothes
le coq sportif	shopping_clothes
le creuset	housing_home
le crobag	food_dining
le meridien	leisure_holidays
le pain quotidien	food_dining
le slip francais	shopping_clothes
le temps des cerises	shopping_clothes
lebenshilfe	shopping_other
leclerc	transport_fuel
lee cooper	shopping_clothes
lee wrangler	shopping_clothes
lego	shopping_other
lego store	shopping_other
legoland	leisure_culture
legoland discovery centre	leisure_culture
leguano	shopping_clothes
leica	shopping_electronics
leifert	food_dining
leiser	shopping_clothes
leiser comfort	shopping_clothes
leonardo boutique	leisure_holidays
leonardo hotels	leisure_holidays
leonardo royal hotel	leisure_holidays
leos	shopping_clothes
leos jeans	shopping_clothes
leroy merlin	housing_home
levis	shopping_clothes
lewiatan	food_groceries
lexus	transport_car_other
lg electronics	shopping_electronics
liberty woman	shopping_clothes
libro	leisure_culture
lidl	food_groceries
lidl deutschland	food_groceries
lidl espana	food_groceries
lidl france	food_groceries
lidl italia	food_groceries
lidl polska	food_groceries
liebeskind	shopping_clothes
liebeskind berlin	shopping_clothes
ligne roset	housing_home
lila backer	food_dining
lincoln	transport_car_other
lindex	shopping_clothes
lipoti pekseg	food_dining
little caesars	food_dining
little caesars pizza	food_dining
little ceasars	food_dining
little ceasars pizza	food_dining
little john bikes	health_sport
liu jo	shopping_clothes
live nation	leisure_culture
livio	food_groceries
ljekarna jadran	health_care
ljekarna joukhadar	health_care
ljekarna pablo	health_care
llaollao	food_dining
lloyd	shopping_clothes
localiza	transport_car_other
loccitane	shopping_drugstore
loccitane en provence	shopping_drugstore
locke	leisure_holidays
loding	shopping_clothes
lody bonano	food_dining
logis	leisure_holidays
logis hotels	leisure_holidays
logo getranke fachmarkt	food_groceries
lohners	food_dining
long john silvers	food_dining
longchamp	shopping_clothes
longines	shopping_other
lonia	food_groceries
loro piana	shopping_clothes
losteria	food_dining
lotos	transport_fuel
lotos optima	transport_fuel
lotteria	food_dining
lotto polska	shopping_other
lotus cars	transport_car_other
louis motorrad	transport_car_other
louis vuitton	shopping_clothes
loving hut	food_dining
lovisa	shopping_other
lowenbacker schaper	food_dining
lpg biomarkt	food_groceries
ltur	leisure_holidays
lubaszka	food_dining
lucid	transport_car_other
lucid motors	transport_car_other
lucid studio	transport_car_other
luckia	shopping_other
luckin	food_dining
luckin coffee	food_dining
luisa spagnoli	shopping_clothes
lukoil	transport_fuel
lukoil minimarket	food_groceries
lukoil shop	food_groceries
lukoil trgovina	food_groceries
lukullus	food_dining
lululemon	shopping_clothes
lumas	leisure_culture
lush	shopping_drugstore
lviv croissants	food_dining
lvm versicherung	financial_insurance
lwowskie croissanty	food_dining
lynk co	transport_car_other
m s foodhall	food_groceries
m s outlet	shopping_other
m s simply food	food_groceries
ma pka express	food_groceries
mac cosmetics	shopping_drugstore
mac geiz	shopping_other
mac oil	transport_car_other
machi machi	food_dining
macs	food_groceries
macs convenience stores	food_groceries
madame coco	housing_home
magyar posta logisztika	shopping_other
magyar posta logisztika mpl	shopping_other
mail boxes etc	shopping_other
maison margiela	shopping_clothes
maisons du monde	housing_home
maje	shopping_clothes
makovec	food_groceries
makro	food_groceries
malin goetz	shopping_drugstore
maloa	food_dining
malzers	food_dining
malzers backstube	food_dining
mama shelter	leisure_holidays
mandarin oriental hotel	leisure_holidays
mandis pharm	health_care
manfield	shopping_clothes
manfield nederland	shopping_clothes
mango	shopping_clothes
manufactum	shopping_other
manufaktura	shopping_drugstore
mapfre	financial_insurance
marathon	transport_fuel
marathon gas	transport_fuel
marble slab creamery	food_dining
marc cain	shopping_clothes
marc jacobs	shopping_clothes
marc opolo	shopping_clothes
marco aldany	health_sport
maredo	food_dining
marella	shopping_clothes
marina rinaldi	shopping_clothes
marionnaud	shopping_drugstore
markant	food_groceries
market basket	food_groceries
markgrafen getrankemarkt	food_groceries
markgrafen getrankevertrieb	food_groceries
markgrafen getrankevertrieb gmbh	food_groceries
marks spencer	food_groceries
marks spencer foodhall	food_groceries
marks spencer outlet	shopping_other
marks spencer simply food	food_groceries
marktkauf	food_groceries
marquardt kuchen	housing_home
marriott	leisure_holidays
marriott autograph collection	leisure_holidays
marriott executive apartments	leisure_holidays
marriott vacation club	leisure_holidays
marrybrown	food_dining
martens	shopping_clothes
martes sport	health_sport
martin auer	food_dining
martin reformstark	food_groceries
martinizing dry cleaning	housing_utilities
martinus	leisure_culture
maserati	transport_car_other
masiarstvo u byka	food_groceries
massimo dutti	shopping_clothes
matratzen concord	housing_home
matt optik	health_care
mavi	shopping_clothes
mavi jeans	shopping_clothes
max co	shopping_clothes
max elektro	shopping_electronics
max fashion	shopping_clothes
max mara	shopping_clothes
max rischart	food_dining
maxi zoo	shopping_other
maxima	food_groceries
maxima xx	food_groceries
maxima xxx	food_groceries
maximarkt	food_groceries
mayers markenschuhe	shopping_clothes
mayersche	leisure_culture
mayoral	shopping_clothes
mazda	transport_car_other
mcarthurglen	shopping_other
mcarthurglen designer outlet	shopping_other
mccafe	food_dining
mcdonalds	food_dining
mcfit	health_sport
mclaren	transport_car_other
mcpaper	shopping_other
mctrek	health_sport
me by melia	leisure_holidays
me hotel	leisure_holidays
me melia	leisure_holidays
mealtime malatang	food_dining
mecklenburgische	financial_insurance
mecklenburgische versicherung	financial_insurance
meda kuchen	housing_home
meda kuchenfachmarkt	housing_home
media expert	shopping_electronics
media home	shopping_electronics
mediamarkt	shopping_electronics
medicine	shopping_clothes
medimax	shopping_electronics
megazoo	shopping_other
meininger	leisure_holidays
melectronics	shopping_electronics
melia	leisure_holidays
melia hotels resorts	leisure_holidays
mephisto	shopping_clothes
mer germany gmbh	transport_fuel
mercator	food_groceries
mercedes	transport_fuel
mercedes benz	transport_fuel
mercure	leisure_holidays
mercure hotels	leisure_holidays
merkur	food_groceries
merkury market	housing_home
merrell	shopping_clothes
merzenich	food_dining
mesopotamia	food_dining
messika	shopping_other
metalac	housing_home
metalac market	housing_home
metro	food_groceries
metro cash carry	food_groceries
metro gastro	food_groceries
metss	food_groceries
mexx	shopping_clothes
mfo matratzen	housing_home
mg motor	transport_car_other
mg motors	transport_car_other
mgallery	leisure_holidays
michael kors	shopping_clothes
midas	transport_car_other
miele	shopping_electronics
mikel	food_dining
mikona	transport_car_other
mila	food_groceries
milk agro	food_groceries
milkau	food_dining
millennium	leisure_holidays
millies cookies	food_dining
mini mix	food_groceries
mini mix markt	food_groceries
minim	food_groceries
miniso	shopping_other
minit	food_dining
mission bbq	food_dining
mister donut	food_dining
mister lady	shopping_clothes
mister minit	housing_utilities
mister spex	health_care
mitsubishi	transport_car_other
mitsubishi motors	transport_car_other
mix markt	food_groceries
mleczarnia jerozolimska	food_dining
mlin i pekare	food_groceries
mlinar	food_dining
mobel boss	housing_home
mobel braun	housing_home
mobel hardeck	housing_home
mobel kraft	housing_home
mobel martin	housing_home
mobel porta	housing_home
mobelix	housing_home
mobil mart	food_groceries
mobiliti	transport_fuel
mobilonline	comm_phone_internet
modepark rother	shopping_clothes
moevenpick	leisure_holidays
mohito	shopping_clothes
moj obchod	food_groceries
mokpol	food_groceries
mol bubi	health_sport
mol nyrt	transport_fuel
mol plugee	transport_fuel
mol shop	food_groceries
momax	housing_home
momoni	shopping_clothes
moncler	shopping_clothes
monki	shopping_clothes
monnari	shopping_clothes
montblanc	shopping_clothes
morawa	leisure_culture
moreboards	shopping_clothes
morphe	shopping_drugstore
moser trachtenwelt	shopping_clothes
motel one	leisure_holidays
mothercare	shopping_other
motivi	shopping_clothes
motrio	transport_car_other
mountain warehouse	health_sport
mountfield	housing_home
movado	shopping_other
movenpick	leisure_holidays
moxy	leisure_holidays
moya	transport_fuel
mpl magyar posta logisztika	shopping_other
mpreis	food_groceries
mr sub	food_dining
mr wash	transport_car_other
mrs sporty	health_sport
ms mode	shopping_clothes
muj obchod	food_groceries
muji	shopping_other
mulberry	shopping_clothes
muller	shopping_drugstore
muller egerer	food_dining
muller und egerer	food_dining
multikino	leisure_culture
multipolster	housing_home
mustang	shopping_clothes
musterring	housing_home
muziker	leisure_culture
mvbike	health_sport
my jewellery	shopping_other
my place	housing_utilities
myflexbox	shopping_other
myplace selfstorage	housing_utilities
myshoes	shopping_clothes
nabibajk	transport_fuel
nacex	shopping_other
naf naf	shopping_clothes
nah frisch	food_groceries
nah gut	food_groceries
nah und frisch	food_groceries
nah und gut	food_groceries
nahkauf	food_groceries
nahrstedt	food_dining
name it	shopping_clothes
nanu nana	charity
napapijri	shopping_clothes
nasz sklep	food_groceries
natur house	shopping_drugstore
nature republic	shopping_drugstore
natuzzi	housing_home
nautica	shopping_clothes
nemzeti dohanybolt	food_dining
neonet	shopping_electronics
neopunkt	shopping_electronics
nespresso	food_dining
neste	transport_fuel
neste oil	transport_fuel
netto	food_groceries
netto city	food_groceries
netto getranke discount	food_groceries
netto marken discount	food_groceries
netto polska	food_groceries
netto salling	food_groceries
neuroth	health_care
new balance	shopping_clothes
new look	shopping_clothes
new style	health_sport
new york pizza	food_dining
new yorker	shopping_clothes
nh collection hotels	leisure_holidays
nh hotel	leisure_holidays
nh hotels	leisure_holidays
nhow	leisure_holidays
nike	shopping_clothes
nike clearance store	shopping_clothes
nike factory store	shopping_clothes
nike well collective	shopping_clothes
nio house	transport_car_other
nio power	transport_fuel
nio power charger	transport_fuel
nio power destination	transport_fuel
nio power swap	transport_fuel
nissan	transport_car_other
nobis	food_dining
nobu	food_dining
nomi	food_groceries
noodle king	food_dining
norauto	transport_car_other
nordoel	transport_fuel
nordsee	food_dining
norma	food_groceries
north sails	shopping_clothes
notebooksbilliger de	shopping_electronics
novotel	leisure_holidays
novus glass	transport_car_other
np markt	food_groceries
nuance	shopping_other
nudie jeans	shopping_clothes
nur hier	food_dining
nurnberger	financial_insurance
nurnberger versicherung	financial_insurance
nuttea	food_dining
o k serwis	transport_car_other
oakberry acai bowls	food_dining
oamtc	transport_car_other
obi	housing_home
occidental	leisure_holidays
ochnik	shopping_clothes
oddzia pzu	financial_insurance
odido	food_groceries
off white	shopping_clothes
offertissima	shopping_other
office depot	shopping_other
office shoes	shopping_clothes
ofotert	health_care
oil vinegar	food_groceries
ok mart	food_groceries
okaidi	shopping_clothes
old chang kee	food_dining
old navy	shopping_clothes
oldboy	health_sport
oldtown white coffee	food_dining
olive garden	food_dining
olive garden italian kitchen	food_dining
oliver peoples	health_care
olkop	transport_fuel
olymp	shopping_clothes
olymp hades	shopping_clothes
olympic casino	shopping_other
omoda	shopping_clothes
omv	transport_fuel
on the run	food_groceries
oneill	shopping_clothes
onezo	food_dining
onezo tapioca	food_dining
onezo tea	food_dining
only sons	shopping_clothes
opel	transport_car_other
opel rent	transport_car_other
oppo	comm_phone_internet
opticalia	health_care
optik matt	health_care
optika mania	health_care
optiker bode	health_care
orange egypt	comm_phone_internet
orchestra	shopping_clothes
oresi	housing_home
oriflame	shopping_drugstore
orlen express	transport_fuel
orlen paczka	shopping_other
orovivo	shopping_other
orsay	shopping_clothes
orterer getrankemarkt	food_groceries
oshkosh	shopping_clothes
oshkosh bgosh	shopping_clothes
osiander	leisure_culture
oskroba	food_dining
ostermann	housing_home
ostrowski rower miejski	health_sport
otacos	food_dining
other stories	shopping_clothes
outback steakhouse	food_dining
outback steakhouse national	food_dining
overseas express hrvatska	shopping_other
ovs kids	shopping_clothes
oxalis	food_dining
oxfam	charity
oxfam books music	charity
oxfam wereldwinkels	charity
oxxo	transport_fuel
oxxo gas	transport_fuel
oyo hotels	leisure_holidays
oyo rooms	leisure_holidays
oysho	shopping_clothes
ozeta	shopping_clothes
packeta	shopping_other
packstation	shopping_other
paczkomat inpost	shopping_other
pagro	shopping_other
paket24	shopping_other
paketbox	shopping_other
palmers	shopping_clothes
pan materac	housing_home
pan pacific	leisure_holidays
pan pek	food_dining
panaceum	health_care
panda express	food_dining
pandora	shopping_other
panerai	shopping_other
panos	food_dining
panta rhei	leisure_culture
papa john	food_dining
papa john pizza	food_dining
papa johns	food_dining
papa johns pizza	food_dining
papparich	food_dining
pappert	food_dining
papperts	food_dining
paprika	shopping_clothes
parcel pending	shopping_other
parfois	shopping_clothes
parfumerie becker	shopping_drugstore
parfumerie pieper	shopping_drugstore
park hyatt	leisure_holidays
park inn	leisure_holidays
park plaza	leisure_holidays
park plaza hotels	leisure_holidays
parkroyal	leisure_holidays
pasibus	food_dining
patagonia	shopping_clothes
patek philippe	shopping_other
paul smith	shopping_clothes
paw owicz	food_dining
payless	transport_car_other
payless shoesource	shopping_clothes
pearle	health_care
pearle opticiens	health_care
pearle optik	health_care
peek cloppenburg	shopping_clothes
pekara	food_dining
pelicana	food_dining
pelicana chicken	food_dining
pendleton	shopping_clothes
penguin box	shopping_other
penguin pharmacy	health_care
penny	food_groceries
penny market	food_groceries
penny markt	food_groceries
penovy raj	transport_car_other
penta hospitals	health_care
penta hospitals sk	health_care
penti	shopping_clothes
pepco	shopping_clothes
pepe jeans	shopping_clothes
pepper lunch	food_dining
pet center	shopping_other
peter hahn	shopping_clothes
peter pane	food_dining
peters gute backstube	food_dining
petit bateau	shopping_clothes
petrol ofisi	transport_fuel
petrolimex	transport_fuel
petronas	transport_fuel
peugeot	transport_car_other
pfennigpfeiffer	shopping_other
phase eight	shopping_clothes
piaget	shopping_other
piana vyshnia	food_dining
picwictoys	shopping_other
piekarenka	food_dining
piekarnia grzybki	food_dining
piekarnia oskroba	food_dining
piekarnia szwajcarska	food_dining
pieprzyk	transport_fuel
pierre cardin	shopping_clothes
pijalnia wodki i piwa	food_dining
pijana wisnia	food_dining
pilulka	health_care
pilulka box	shopping_other
pimkie	shopping_clothes
pingvin patika	health_care
pinkberry	food_dining
pinko	shopping_clothes
pinky club	shopping_other
pita pit	food_dining
pitstop	transport_car_other
pittarosso	shopping_clothes
pizza hut	food_dining
pizza hut delivery	food_dining
pizza hut express	food_dining
pizza inn	food_dining
pizza max	food_dining
pizza max deutschland	food_dining
pizza nova	food_dining
pizzeria 105	food_dining
plana kuchenland	housing_home
planeo elektro	shopping_electronics
playgosmart	shopping_electronics
pletzsch	shopping_other
plodine	food_groceries
plus lekaren	health_care
pneuhage	transport_car_other
pneumobil	transport_car_other
poco	housing_home
poczta polska	shopping_other
poczta polska sa	shopping_other
pocztex	shopping_other
pod telegrafem	food_dining
pofam poznan	health_care
poggenpohl	housing_home
point s	transport_car_other
polaris	transport_car_other
polestar	transport_car_other
polestar racing	transport_car_other
pollo campero	food_dining
pollo tropical	food_dining
polo motorrad	transport_car_other
polo ralph lauren	shopping_clothes
polomarket	food_groceries
poltrona frau	housing_home
pom dapi	shopping_clothes
pommesfreunde	food_dining
pompo	shopping_other
popeyes	food_dining
porsche	transport_fuel
porsche design	shopping_clothes
porta	housing_home
porta einrichtungshaus	housing_home
porta kuchenwelt	housing_home
post abholstation	shopping_other
postenborse	shopping_other
posto shell	transport_fuel
poststation	shopping_other
potato corner	food_dining
potraviny tempo	food_groceries
pottery barn	housing_home
pottery barn kids	housing_home
powerbox one	transport_fuel
powerdot	transport_fuel
"""

    private const val CHUNK_1 = """
powszechny zak ad ubezpieczen	financial_insurance
ppl parcelbox	shopping_other
prada	shopping_clothes
praktiker	housing_home
preem	transport_fuel
premier inn	leisure_holidays
premiere classe	leisure_holidays
premio	transport_car_other
prespanok	housing_home
press books	leisure_culture
pret a manger	food_dining
prim	transport_fuel
prima pharme	health_care
primark	shopping_clothes
primark penneys	shopping_clothes
princesse tam tam	shopping_clothes
privat max	food_groceries
pro optik	health_care
promod	shopping_clothes
prospanek	housing_home
provinzial	financial_insurance
provinzial nordwest	financial_insurance
provinzial rheinland	financial_insurance
prudential	financial_insurance
przyjazna	health_care
przystanek piekarnia	food_dining
ps fashion	shopping_clothes
ps paketomat	shopping_other
psb mrowka	housing_home
ptt station	transport_fuel
pull bear	shopping_clothes
pullman	leisure_holidays
puma	transport_fuel
puma energy	transport_fuel
puma outlet	shopping_clothes
punt roma	shopping_clothes
pupa milano	shopping_drugstore
pustet	leisure_culture
putka	food_dining
pv automotive	transport_car_other
pzu s a	financial_insurance
qb house	health_sport
qubus	leisure_holidays
quick reifendiscount	transport_car_other
quick schuh	shopping_clothes
quickly	food_dining
quicksilver	shopping_clothes
quiksilver	shopping_clothes
quiktrip	transport_fuel
quiktrip corporation	transport_fuel
quiosque	shopping_clothes
quiznos	food_dining
quiznos sub	food_dining
quiznos subs	food_dining
r m williams	shopping_clothes
r v versicherung	financial_insurance
r v versicherungen	financial_insurance
raab karcher	housing_home
rabat	food_groceries
rabat detal	food_groceries
racetrac	transport_fuel
radioshack	shopping_electronics
radisson	leisure_holidays
radisson blu	leisure_holidays
radisson collection	leisure_holidays
radisson hotels	leisure_holidays
radisson hotels americas	leisure_holidays
radisson red	leisure_holidays
raffles	leisure_holidays
raiffeisen	transport_fuel
raiffeisen baucenter	housing_home
raiffeisen markt	housing_home
raiffeisen und volksbanken versicherung	financial_insurance
rainbow	shopping_clothes
rainbow shops	shopping_clothes
ramada	leisure_holidays
rapha	health_sport
ray ban	health_care
real pont	food_groceries
realk	transport_fuel
red lobster	food_dining
red wing	shopping_clothes
red wing shoes	shopping_clothes
red zac	shopping_electronics
reddy kuchen	housing_home
reebok	shopping_clothes
reform martin	food_groceries
reformhaus bacher	food_groceries
reformhaus engelhardt	food_groceries
reformstark martin	food_groceries
regatta great outdoors	health_sport
regiorad stuttgart	health_sport
regus	work_tools
reifen com	transport_car_other
reiseland	leisure_holidays
reisezentrum	leisure_culture
relais chateaux	leisure_holidays
relay	food_dining
renaissance	leisure_holidays
renaissance hotel	leisure_holidays
renault	transport_car_other
reno	shopping_clothes
repsol	transport_fuel
reserved	shopping_clothes
residence inn	leisure_holidays
reunokoria	transport_car_other
revolution laundry	housing_utilities
rewe	food_groceries
rewe city	food_groceries
rewe getrankemarkt	food_groceries
rewe to go	food_groceries
rhg bau garten	housing_home
rhg baustoffe	housing_home
ribola	food_groceries
richter erzgebirge	food_groceries
richters altstadt backerei	food_dining
rieker	shopping_clothes
rimac	transport_car_other
rimowa	shopping_clothes
rindchens weinkontor	food_groceries
ringhotels	leisure_holidays
risa chicken	food_dining
rischart	food_dining
rischarts backhaus	food_dining
rituals	shopping_drugstore
rituals cosmetics	shopping_drugstore
ritz	leisure_holidays
ritz carlton	leisure_holidays
riu paris	shopping_clothes
river island	shopping_clothes
rixos	leisure_holidays
rixos hotels	leisure_holidays
robel	shopping_clothes
robert bosch gmbh	transport_fuel
roberto cavalli	shopping_clothes
roche bobois	housing_home
rockport	shopping_clothes
rodzinna	health_care
rofu kinderland	shopping_other
roger dubuis	shopping_other
rolex	shopping_other
rolf benz	housing_home
rolls royce	transport_car_other
rolls royce motor cars	transport_car_other
romantik	food_dining
romantik hotel	leisure_holidays
romantik hotels	leisure_holidays
romantik hotels restaurants	food_dining
romantik restaurant	food_dining
rosewood	leisure_holidays
rossmann	shopping_drugstore
rossmann express	shopping_drugstore
rossmann polska	shopping_drugstore
rotes kreuz	health_care
royal donuts	food_dining
royal kebab	food_dining
royaltea	food_dining
rtv euro agd	shopping_electronics
rubis	transport_fuel
ruby tuesday	food_dining
ruch	leisure_culture
ruefa	leisure_holidays
ruetz	food_dining
rupprecht	leisure_culture
ruprecht	leisure_culture
rusta	shopping_other
rutar	housing_home
rwe	transport_fuel
s oliver	shopping_clothes
s oneczko	food_groceries
s oneczna	health_care
s strene grene	housing_home
sabon	shopping_drugstore
sae institute	leisure_education
safeway	transport_fuel
safeway fuel station	transport_fuel
sagasser	food_groceries
saint algue	health_sport
saint gobain betriebskrankenkasse	financial_insurance
saint laurent	shopping_clothes
saizeriya	food_dining
salamander	shopping_clothes
salomon	shopping_clothes
salvatore ferragamo	shopping_clothes
sam 73	shopping_clothes
sams e sams e	shopping_clothes
samsoe samsoe	shopping_clothes
samsonite	shopping_clothes
samsung	shopping_electronics
san antonio shoe	shopping_clothes
sanders backstube	food_dining
saravana bhavan	food_dining
saravanaa bhavan	food_dining
sas san antonio shoemakers	shopping_clothes
sas shoes	shopping_clothes
sassoon salon	health_sport
satur	leisure_holidays
saturn	shopping_electronics
sausalitos	food_dining
sb mobel boss	housing_home
sb tank	transport_fuel
sbarro	food_dining
sbarro pizzeria	food_dining
scandic	leisure_holidays
scandic hotels	leisure_holidays
schafers	food_dining
schaffrath	housing_home
schaper	food_dining
scheck in center	food_groceries
schiesser	shopping_clothes
schneider	health_care
schuback	shopping_drugstore
schuback parfumerien	shopping_drugstore
schuh mann	shopping_clothes
schuhkay	shopping_clothes
schuhpark	shopping_clothes
schweinske	food_dining
schwerdtner	food_dining
sconto mobel sofort	housing_home
sconto nabytek	housing_home
sconto sb	housing_home
scotch soda	shopping_clothes
screwfix	housing_home
seat	transport_car_other
seats and sofas	housing_home
seattles best coffee	food_dining
sebago	shopping_clothes
second cup	food_dining
sedal	food_groceries
segafredo	food_dining
segmuller	housing_home
sehen wutscher	health_care
sehne	food_dining
seidensticker	shopping_clothes
seiko	shopping_other
selected	shopping_clothes
selgros	food_groceries
selgros cash carry	food_groceries
selina	leisure_holidays
seneca tours	leisure_holidays
sensiblu	health_care
sephora	shopping_drugstore
sergent major	shopping_clothes
servicestore db	shopping_other
seven eleven	transport_fuel
sevt	shopping_other
sezane	shopping_clothes
sfera	shopping_clothes
shake shack	food_dining
shakeys	food_dining
shane english school	leisure_education
sharetea	food_dining
shell	transport_fuel
shell australia	transport_fuel
shell bkk life	financial_insurance
shell car wash	transport_car_other
shell express	transport_fuel
shell gas station	transport_fuel
shell oil	transport_fuel
shell petrol station	transport_fuel
shell service station	transport_fuel
shell shop	food_groceries
shell station	transport_fuel
sheraton	leisure_holidays
sherwin williams	housing_home
sherwin williams paint store	housing_home
sherwin williams paints	housing_home
shiseido	shopping_drugstore
shoe4you	shopping_clothes
shoo loong kan	food_dining
shurgard	housing_utilities
shurgard self storage	housing_utilities
shurgard storage centers	housing_utilities
sidestep	shopping_clothes
siematic	housing_home
siemes schuhcenter	shopping_clothes
sigikid	shopping_clothes
signal iduna	financial_insurance
sigo	health_sport
sigo e lastenrad sharing	health_sport
sigo e lastenrad standort	health_sport
siko	housing_home
simpo	housing_home
sinnleffers	shopping_clothes
sinsay	shopping_clothes
sisley	shopping_clothes
six senses	leisure_holidays
sixt	transport_car_other
sixt rent a car	transport_car_other
sizeer	shopping_clothes
skechers	shopping_clothes
skechers usa	shopping_clothes
sketchers	shopping_clothes
sklep miesny lenarcik	food_groceries
sklep polski	food_groceries
skoda	transport_car_other
skoda auto	transport_car_other
slawex	food_groceries
sligro	food_groceries
slovnaft	transport_fuel
smartshop	comm_phone_internet
smiggle	shopping_other
smileys	food_dining
smileys pizza	food_dining
smileys pizza profis	food_dining
smyk	shopping_other
smyths	shopping_other
smyths toys	shopping_other
snipes	shopping_clothes
sobi	food_groceries
socar	transport_fuel
socialna poistovna	financial_insurance
sofitel	leisure_holidays
sofly	food_dining
soko ow	food_groceries
sonder	leisure_holidays
sonderpreis baumarkt	housing_home
sonic drive in	food_dining
sonnentor	food_dining
sony	shopping_electronics
sony center	shopping_electronics
sony centre	shopping_electronics
spar express	food_groceries
spar gourmet	food_groceries
spar ni	food_groceries
specialized	health_sport
specsavers	health_care
specsavers opticians	health_care
speed queen	housing_utilities
sphinx	food_dining
spiele max	shopping_other
spo em	food_groceries
sport 2000	health_sport
sport tiedje	health_sport
sport vision	health_sport
sportisimo	health_sport
sportler	health_sport
sports authority	health_sport
sports direct	health_sport
sportscheck	health_sport
sportsdirect com	health_sport
sportsdirect dot com	health_sport
springfield	shopping_clothes
srm jasko ka	health_sport
ssangyong	transport_car_other
st christophers inn	leisure_holidays
st regis	leisure_holidays
sta travel	leisure_holidays
stabilita	financial_insurance
stadium	health_sport
stadium sports store	health_sport
stadler	health_sport
stadtbackerei kamp	food_dining
stadtparfumerie pieper	shopping_drugstore
stadtrad hamburg	health_sport
stalowa wola miasto rowerow	health_sport
stangengruner muhlenbackerei	food_dining
star mart	food_groceries
starbucks	food_dining
starbucks canada	food_dining
starbucks uk	food_dining
starcar	transport_car_other
starke backer	food_dining
station avia	transport_fuel
station esso	transport_fuel
station service	transport_fuel
station service e leclerc	transport_fuel
station shell	transport_fuel
station total	transport_fuel
statoil	transport_fuel
stavmat epitoanyag kereskedelem	housing_home
stavmat stavebniny	housing_home
stavmat stavebniny cesko	housing_home
stavmat stavebniny slovensko	housing_home
staycity	leisure_holidays
staycity aparthotel	leisure_holidays
staycity aparthotels	leisure_holidays
stefanel	shopping_clothes
steiff	shopping_other
steinecke	food_dining
steiskal	food_dining
stella mccartney	shopping_clothes
sternenback	food_dining
stihl	housing_home
stinges	food_dining
stinges landbackerei	food_dining
stokrotka	food_groceries
stokrotka express	food_groceries
stokrotka market	food_groceries
stokrotka optima	food_groceries
stop cafe	food_groceries
storck	food_dining
storck welt outlet	food_dining
storebox	housing_utilities
stradivarius	shopping_clothes
street one	shopping_clothes
street shoes	shopping_clothes
strock	food_dining
stuart weitzman	shopping_clothes
studenac	food_groceries
stylebox	health_sport
stylebox klier	health_sport
styleboxx	health_sport
styleboxx klier	health_sport
suavinex	shopping_other
subaru	transport_car_other
subway	food_dining
subway australia	food_dining
subway brasil	food_dining
subway mexico	food_dining
subway sandwiches	food_dining
subway uk ireland	food_dining
suitsupply	shopping_clothes
sunglass hut	health_care
sunpoint	health_sport
super c	food_groceries
super cut	health_sport
super pharm	health_care
super zoo	shopping_other
superbet	shopping_other
superbiomarkt	food_groceries
superdry	shopping_clothes
supermarket slawex	food_groceries
superwash	transport_car_other
sure hotel	leisure_holidays
sure hotel collection	leisure_holidays
sushi shop	food_dining
sushi shop europe	food_dining
sushi story	food_dining
sushi wok	food_dining
sutterluty	food_groceries
suzuki	transport_car_other
suzuki motorcycle india	transport_car_other
sv sparkassenversicherung	financial_insurance
svet zdravia	health_care
swapfiets	health_sport
swarovski	shopping_other
swatch	shopping_other
sweaty betty	shopping_clothes
swiat alkoholi	food_groceries
swiat ksiazki	leisure_culture
swiat prasy	leisure_culture
swiezyzna	food_groceries
swing kitchen	food_dining
swiss belhotel	leisure_holidays
swiss belinn	leisure_holidays
swiss life	financial_insurance
swiss sense	housing_home
swissotel	leisure_holidays
synevo	health_care
synevo polska	health_care
synlab	health_care
szimpatika	health_care
t mobile	comm_phone_internet
t mobile international	comm_phone_internet
t mobile pl	comm_phone_internet
t4 tea for u	food_dining
t4 tea house	food_dining
tabak press	leisure_culture
tabak traficon	food_dining
tabaktrafik	food_dining
taco bell	food_dining
tag heuer	shopping_other
takko fashion	shopping_clothes
tally weijl	shopping_clothes
tam autohof	transport_fuel
tamaris	shopping_clothes
tamburi	shopping_other
tamoil	transport_fuel
tamoil italia spa	transport_fuel
tank ono	transport_fuel
tanker	transport_fuel
tankpool24	transport_fuel
tape a l il	shopping_clothes
tartine et chocolat	shopping_clothes
tatuum	shopping_clothes
tcby	food_dining
tchibo	food_dining
techniker krankenkasse	financial_insurance
ted baker	shopping_clothes
tedi	shopping_other
tedox	housing_home
teeamo	food_dining
teegschwendner	food_dining
tegut	food_groceries
tele2	comm_phone_internet
telekom	comm_phone_internet
telekom shop	comm_phone_internet
telepizza	food_dining
tempo cesko	food_groceries
tempo jo	food_groceries
tempo market	food_groceries
tempo market potraviny	food_groceries
tempo slovensko	food_groceries
tempo tuty	food_groceries
tempur	housing_home
terno	food_groceries
terranova	shopping_clothes
tesco	food_groceries
tesco esso express	transport_fuel
tesco expres	food_groceries
tesco express	food_groceries
tesco extra	food_groceries
tesco metro	food_groceries
tesco petrol filling station	transport_fuel
tesco petrol station	transport_fuel
tesco superstore	food_groceries
tescoma	housing_home
tesla	transport_fuel
tesla destination charger	transport_fuel
tesla motors	transport_fuel
tesla motors inc	transport_fuel
tesla supercharger	transport_fuel
tesla wall connector	transport_fuel
teta	shopping_drugstore
teufel	shopping_electronics
texaco	transport_fuel
texaco service station	transport_fuel
texaco with techron	transport_fuel
textile house	shopping_clothes
tezenis	shopping_clothes
tgi fridays	food_dining
thalia	leisure_culture
the athletes foot	shopping_clothes
the body shop	shopping_drugstore
the chicken rice shop	food_dining
the coffee	food_dining
the gap	shopping_clothes
the hoxton	leisure_holidays
the kooples	shopping_clothes
the langham	leisure_holidays
the luxury collection	leisure_holidays
the niu	leisure_holidays
the north face	shopping_clothes
the ritz	leisure_holidays
the ritz carlton	leisure_holidays
the sting	shopping_clothes
the walking company	shopping_clothes
the westin	leisure_holidays
thom browne	shopping_clothes
thomas cook	leisure_holidays
thomas philipps	shopping_other
thomas philipps sonderposten	shopping_other
thomas sabo	shopping_other
thrifty	transport_car_other
thrifty car rental	transport_car_other
ticketmaster	leisure_culture
tiffany	shopping_other
tiffany company	shopping_other
tiffanys	shopping_other
tiger mart	food_groceries
tiger sugar	food_dining
tim hortons	food_dining
timberland	shopping_clothes
tink	health_sport
tink konstanz	health_sport
tinq	transport_fuel
tip travel	leisure_holidays
tipico	shopping_other
tipsport	shopping_other
tisak	shopping_other
tissot	shopping_other
titi	shopping_other
titi papiernictvo	shopping_other
tk maxx	shopping_other
tods	shopping_clothes
tokic	transport_car_other
tokyobike	health_sport
tom ford	shopping_clothes
tom market	food_groceries
tom tailor	shopping_clothes
tommy	food_groceries
tommy hilfiger	shopping_clothes
tommy hilfiger kids	shopping_clothes
toni guy	health_sport
tony roma	food_dining
tony romas	food_dining
toom baumarkt	housing_home
top drogerie	shopping_drugstore
top hair	health_sport
top market	food_groceries
top secret	shopping_clothes
top shop	shopping_other
topaz	food_groceries
topgolf	health_sport
topman	shopping_clothes
topshop	shopping_clothes
torvelo	health_sport
total access	transport_fuel
total wash	transport_car_other
totalenergies	transport_fuel
totalenergies access	transport_fuel
totalenergies charging services	transport_fuel
totalerg	transport_fuel
totolotek	shopping_other
tous	shopping_other
toyoko inn	leisure_holidays
toyota	transport_car_other
toyota usa	transport_car_other
trademark	leisure_holidays
trademark by wyndham	leisure_holidays
trademark collection	leisure_holidays
trademark wyndham	leisure_holidays
traficon	food_dining
transgourmet	food_groceries
trauerhilfe denk	others
travelplanet pl	leisure_holidays
treck	health_sport
tredy	shopping_clothes
trek	health_sport
trek bicycle	health_sport
trgocentar	food_groceries
trgostil	food_groceries
tribute portfolio	leisure_holidays
trigema	shopping_clothes
triumph	transport_car_other
tryp	leisure_holidays
tsutaya	leisure_culture
tudor	shopping_other
tuerkis	food_dining
tui group	leisure_holidays
tui reisecenter	leisure_holidays
tumi	shopping_clothes
turancar	leisure_holidays
turkis	food_dining
turkis city	food_dining
turkish airlines	leisure_holidays
turmol	transport_fuel
tuty	food_groceries
tuv hanse	transport_car_other
tuv hessen	transport_car_other
tuv nord	transport_car_other
tuv rheinland	transport_car_other
tuv saarland	transport_car_other
tuv sud	transport_car_other
tuv thuringen	transport_car_other
twg tea	food_dining
twoj market	food_groceries
u s polo	shopping_clothes
u s polo assn	shopping_clothes
u store	food_groceries
ubezpieczenia pzu	financial_insurance
ubitricity	transport_fuel
ubitricity gmbh	transport_fuel
uci kinowelt	leisure_culture
ugg ugg australia	shopping_clothes
ulla popken	shopping_clothes
ultramar	transport_fuel
umbrella	food_dining
unbound	leisure_holidays
unbound collection	leisure_holidays
unbound hyatt	leisure_holidays
uncle tetsus cheesecake	food_dining
under armor	shopping_clothes
under armour	shopping_clothes
under armour youth	shopping_clothes
uni hobby	housing_home
unilink	financial_insurance
unimarkt	food_groceries
union poistovna	financial_insurance
union zdravotna poistovna	financial_insurance
uniqa	financial_insurance
uniqa asigurari	financial_insurance
uniqlo	shopping_clothes
united colors of benetton	shopping_clothes
united parcel service	shopping_other
universa	financial_insurance
urban outfitters	shopping_clothes
v baumarkt	housing_home
v markt	food_groceries
vacheron constantin	shopping_other
valentino	shopping_clothes
valero	transport_fuel
valmano	shopping_other
van der valk	leisure_holidays
van der valk hotel	leisure_holidays
van graaf	shopping_clothes
van heusen	shopping_clothes
van laack	shopping_clothes
vanessa bruno	shopping_clothes
vans	shopping_clothes
vapiano	food_dining
vasa	health_care
vasa lekaren	health_care
vase zdravlje	health_care
vatsak	food_dining
vatsak confectionery house	food_dining
vaude	health_sport
vauxhall	transport_car_other
vean tattoo	health_sport
veganista	food_dining
veggie pret	food_dining
velocity aachen	health_sport
vereinigte ikk	financial_insurance
vergolst	transport_car_other
vero moda	shopping_clothes
versace	shopping_clothes
vertbaudet	shopping_clothes
vespa	transport_car_other
vianor	transport_car_other
viba	food_dining
victor kvety	housing_home
victorias secret	shopping_clothes
vienna house	leisure_holidays
vila	shopping_clothes
vilebrequin	shopping_clothes
villeroy boch	housing_home
vincent	food_dining
vinzenzmurr	food_groceries
vision express	health_care
vistula	shopping_clothes
vitakustik	health_care
vitalia	food_groceries
vitalia reformhaus	food_groceries
viva billa	food_groceries
vivo	comm_phone_internet
voco	leisure_holidays
vodafone	comm_phone_internet
vodafone deutschland	comm_phone_internet
vodafone it	comm_phone_internet
vodafone italia spa	comm_phone_internet
vodafone shop	comm_phone_internet
vogele shoes	shopping_clothes
volcom	shopping_clothes
volkssolidaritat	shopping_other
volkswagen	transport_car_other
volkswagen commercial vehicles	transport_car_other
volvo	transport_car_other
vomfass	food_groceries
von allworden	food_dining
vseobecna zdravotna poistovna	financial_insurance
w hotels	leisure_holidays
w kruk	shopping_other
wacoal	shopping_clothes
wafelek	food_groceries
wagamama	food_dining
wagrowiecki rower miejski	health_sport
wahlburgers	food_dining
wajos	food_groceries
wakacje pl	leisure_holidays
walbusch	shopping_clothes
waldorf astoria	leisure_holidays
wall street english	leisure_education
walmart	transport_fuel
walther konig	leisure_culture
warhammer	leisure_culture
warta	financial_insurance
wasgau	food_groceries
wash me	housing_utilities
wash totalenergies	transport_car_other
watis	transport_fuel
watsons	health_care
wayback burgers	food_dining
we fashion	shopping_clothes
weekday	shopping_clothes
weekend max mara	shopping_clothes
well booked	leisure_holidays
wellensteyn	shopping_clothes
wellyou	health_sport
weltbild	leisure_culture
wempe	shopping_other
wendys	food_dining
weso a pani	food_groceries
west elm	housing_home
westfalen	transport_fuel
westfield	shopping_other
westfield europe usa	shopping_other
westin	leisure_holidays
wework	work_tools
white stuff	shopping_clothes
whsmith	leisure_culture
wiener feinbacker	food_dining
wiener feinbacker heberer	food_dining
wiener feinbackerei	food_dining
wiener feinbackerei heberer	food_dining
wiener stadtische versicherung	financial_insurance
wienerroither	food_dining
wierzejki	food_groceries
wiky	shopping_other
wild bean cafe	food_dining
wilma wunder	food_dining
wimpy	food_dining
wimpy restaurants	food_dining
wintec autoglas	transport_car_other
witt weiden	shopping_clothes
wm fahrzeugteile	transport_car_other
wohrl	shopping_clothes
wojas	shopping_clothes
wolczanka	shopping_clothes
wolford	shopping_clothes
wolsdorf	food_dining
wolsdorff	food_dining
wolsdorff tobacco	food_dining
womens secret	shopping_clothes
womensecret	shopping_clothes
wonderwaffel	food_dining
woolworth	shopping_other
woolworth deutschland	shopping_other
world duty free	shopping_other
world gym	health_sport
worldbox	shopping_clothes
worldhotel	leisure_holidays
worldhotels	leisure_holidays
wreesmann	shopping_other
wreesmann sonderpostenmarkt	shopping_other
wunsche	food_dining
wurth	housing_home
wurttembergische	financial_insurance
wustenrot	financial_insurance
wyjatkowy prezent	charity
wyndham	leisure_holidays
wyndham garden	leisure_holidays
wyndham grand	leisure_holidays
wyndham hotel	leisure_holidays
x kom	shopping_electronics
xenos	housing_home
xiaolongkan	food_dining
xiaomi	comm_phone_internet
xing fu tang	food_dining
xpeng	transport_car_other
xxxlutz	housing_home
yamaha	transport_car_other
yatas	housing_home
yatas bedding	housing_home
yellowkorner	leisure_culture
yeme	food_groceries
yi fang	food_dining
yi fang fruit tea	food_dining
yi fang tea	food_dining
ymca	health_sport
ymca shop	charity
yogen fruz	food_dining
yogorino	food_dining
yormas	food_groceries
yourfone	comm_phone_internet
yourphone	comm_phone_internet
yours	shopping_clothes
yours clothing	shopping_clothes
yves rocher	shopping_drugstore
z box	shopping_other
zabka	food_groceries
zadig voltaire	shopping_clothes
zagrebacke pekarne klara	food_dining
zahir kebab	food_dining
zappka	food_groceries
zara	shopping_clothes
zara home	housing_home
zarskie rowery	health_sport
zdrofit	health_sport
zdrowie	health_care
zeeman	shopping_clothes
zegna	shopping_clothes
zeit fur brot	food_dining
zeppelin cat	housing_home
zg raiffeisen	housing_home
zg raiffeisen agrar	housing_home
zg raiffeisen baustoffe	housing_home
zg raiffeisen energie	transport_fuel
zg raiffeisen markt	housing_home
zg raiffeisen technik	housing_home
ziaja	shopping_drugstore
ziarenko	food_dining
ziko apteka	health_care
zillertaler trachtenwelt	shopping_clothes
zilli	shopping_clothes
zlatarna celje	shopping_other
zleep hotel	leisure_holidays
zleep hotels	leisure_holidays
zoo co	shopping_other
zse drive	transport_fuel
zurbruggen	housing_home
zweirad center stadler	health_sport
zwilling	housing_home
"""
}
