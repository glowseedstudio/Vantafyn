package dev.vantafyn.feature.home.games.pokemon

/**
 * Registry of Pokémon species exhibiting canonical visual gender differences across Generations 1–9.
 *
 * PokeAPI hosts female variants for these 101 species in:
 * - Pixel sprites: sprites/pokemon/female/{speciesId}.png (and shiny/female/)
 * - Pokémon HOME 3D renders: sprites/pokemon/other/home/female/{speciesId}.png (and other/home/shiny/female/)
 */
object PokemonGenderCatalog {

    val GENDER_DIFFERENCE_SPECIES_IDS: Set<Int> = setOf(
        3, 12, 19, 20, 25, 26, 41, 42, 44, 45, 64, 65, 84, 85, 97, 111, 112, 118, 119, 123, 129, 130, 133,
        154, 165, 166, 178, 185, 186, 190, 194, 195, 198, 202, 203, 207, 208, 212, 214, 215, 217, 221, 224, 229, 232,
        255, 256, 257, 267, 269, 272, 274, 275, 307, 308, 315, 316, 317, 322, 323, 332, 350, 369,
        396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 407, 415, 417, 418, 419, 424, 443, 444, 445, 449, 450, 453, 454, 456, 457, 459, 460, 461, 464, 465, 473,
        521, 592, 593,
        668, 678,
        876, 916
    )

    fun hasGenderDifferences(speciesId: Int): Boolean = speciesId in GENDER_DIFFERENCE_SPECIES_IDS

    fun getGenderDifferenceDescription(speciesId: Int): String? {
        return when (speciesId) {
            3 -> "Female has a yellow seed in the center of its flower"
            12 -> "Female has black circular spots on lower wings"
            19, 20 -> "Female has shorter whiskers"
            25 -> "Female has a heart-shaped indentation at the end of its tail"
            26 -> "Female has a blunt, flattened tail tip"
            41, 42 -> "Female has smaller fangs"
            44 -> "Female has one large spot on each petal"
            45 -> "Female has fewer but larger flower petal spots"
            64, 65 -> "Female has a significantly shorter mustache"
            84, 85 -> "Female has brown necks instead of black"
            97 -> "Female has longer white collar fur"
            111, 112 -> "Female has a smaller horn"
            118, 119 -> "Female has a smaller horn"
            123 -> "Female has a larger abdomen"
            129 -> "Female has white whiskers instead of yellow"
            130 -> "Female has white whiskers instead of blue"
            133 -> "Female tail fur tip features a heart/floral silhouette"
            154 -> "Female has shorter head antennae"
            165, 166 -> "Female has smaller back spots"
            178 -> "Female has one yellow stripe across chest"
            185 -> "Female has a smaller head branch"
            186 -> "Female has smaller pink cheeks"
            190 -> "Female has shorter head hair tufts"
            194 -> "Female has one set of gill branches"
            195 -> "Female has a smaller dorsal fin"
            198 -> "Female has a smaller head crest feather"
            202 -> "Female wears bright red lipstick"
            203 -> "Female has a smaller dark body patch"
            207 -> "Female has a smaller tail stinger"
            208 -> "Female has fewer outer jagged teeth"
            212 -> "Female has a noticeably larger abdomen"
            214 -> "Female has a heart-shaped horn instead of a cross"
            215 -> "Female has a shorter red feather ear"
            217 -> "Female has longer shoulder fur"
            221 -> "Female has shorter tusks"
            224 -> "Female has smaller suction cups"
            229 -> "Female has shorter curved horns"
            232 -> "Female has shorter tusks"
            255 -> "Male has a tiny black speck on rear; female does not"
            256, 257 -> "Female has shorter head feathers"
            267 -> "Female has smaller red spots on upper wings"
            269 -> "Female has smaller antennae"
            272 -> "Female has thinner zig-zag body stripes"
            274, 275 -> "Female has smaller leaves"
            307, 308 -> "Female has smaller head ears / bulb"
            315 -> "Female has a larger skirt leaf"
            316, 317 -> "Female has a shorter head feather / whiskers"
            322, 323 -> "Female has a larger back hump"
            332 -> "Female has a large diamond pattern on chest"
            350 -> "Female has longer pink hair fins"
            369 -> "Female has shorter jaw whiskers"
            396, 397, 398 -> "Female has a smaller white forehead marking"
            399 -> "Female has 3 tail bumps instead of 5"
            400 -> "Female has a darker face mask pattern"
            401, 402 -> "Female has a larger collar / smaller mustache"
            403, 404, 405 -> "Female has blue hind feet and shorter mane"
            407 -> "Female has a longer flowing cape"
            415 -> "Female has a red jewel on lower forehead (only female evolves to Vespiquen)"
            417 -> "Female has a shorter forehead stripe"
            418, 419 -> "Female has one white spot on back instead of two"
            424 -> "Female has shorter head hair"
            443, 444, 445 -> "Male has a notched dorsal fin; female has a smooth fin"
            449 -> "Female has inverted colors (darker body, tan spots)"
            450 -> "Female has dark charcoal/black skin instead of sandy tan"
            453, 454 -> "Female has higher white chest markings / smaller throat sac"
            456, 457 -> "Female has larger lower fins"
            459, 460 -> "Female has white midsection / longer chest fur"
            461 -> "Female has shorter red ear feathers"
            464 -> "Female has a smaller horn"
            465 -> "Female has red ringed finger tips"
            473 -> "Female has shorter tusks"
            521 -> "Male has extravagant head plumage; female is understated brown"
            592 -> "Male is blue with diamond frills; female is pink with wavy ruffles"
            593 -> "Male is blue with a mustache; female is pink with collar and crown"
            668 -> "Male has a full fiery lion mane; female has a flowing flame ponytail"
            678 -> "Male is navy blue with white tips; female is white with navy tips"
            876 -> "Male has upward horns; female has downward curving horns"
            916 -> "Male has yellow eyes and dark skin; female has pink hooded eyes and brown coat"
            else -> null
        }
    }

    /**
     * Resolves the canonical high-definition Pokémon HOME 3D render for the Pokédex presentation.
     * Ensures consistent 3D styling across default male, female, shiny, and alternate forms.
     */
    fun getPokedexArtworkUrl(
        speciesId: Int,
        isShiny: Boolean = false,
        isFemale: Boolean = false,
        formKey: String? = null,
    ): String {
        if (speciesId <= 0) return ""
        val hasDiff = if (!formKey.isNullOrBlank()) false else hasGenderDifferences(speciesId)

        if (!formKey.isNullOrBlank()) {
            return if (isShiny) {
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/shiny/$formKey.png"
            } else {
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/$formKey.png"
            }
        }

        return when {
            isShiny && isFemale && hasDiff ->
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/shiny/female/$speciesId.png"
            isShiny ->
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/shiny/$speciesId.png"
            isFemale && hasDiff ->
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/female/$speciesId.png"
            else ->
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/$speciesId.png"
        }
    }
}
