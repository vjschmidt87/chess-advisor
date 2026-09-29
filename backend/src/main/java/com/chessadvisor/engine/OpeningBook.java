package com.chessadvisor.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Opening recognition database mapping move sequences to opening names and ECO codes.
 * Contains 30+ common chess openings.
 */
public class OpeningBook {

    /**
     * Maps a move sequence key (space-separated SAN moves) to [opening name, ECO code].
     */
    private final Map<String, String[]> openings;

    public OpeningBook() {
        openings = new HashMap<>();
        initializeOpenings();
    }

    /**
     * Identify the opening from a list of SAN move strings.
     * Tries the longest match first, working backwards to find the most specific opening.
     *
     * @param moves list of SAN moves (e.g., ["e4", "c5", "Nf3", "d6"])
     * @return Optional containing [opening name, ECO code], or empty if not recognized
     */
    public Optional<String[]> identifyOpening(List<String> moves) {
        // Try longest prefix match first for the most specific name
        for (int len = moves.size(); len >= 1; len--) {
            String key = buildKey(moves, len);
            // Normalize castling notation
            key = normalizeCastling(key);
            String[] result = openings.get(key);
            if (result != null) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    /**
     * Get all stored openings (for debugging/listing).
     */
    public Map<String, String[]> getAllOpenings() {
        return new HashMap<>(openings);
    }

    private String buildKey(List<String> moves, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(moves.get(i));
        }
        return sb.toString();
    }

    private String normalizeCastling(String key) {
        return key.replace("0-0-0", "O-O-O").replace("0-0", "O-O");
    }

    private void initializeOpenings() {
        // === OPEN GAMES (1. e4 e5) ===

        // Italian Game
        put("e4 e5 Nf3 Nc6 Bc4", "Italian Game", "C50");
        put("e4 e5 Nf3 Nc6 Bc4 Bc5", "Italian Game: Giuoco Piano", "C53");
        put("e4 e5 Nf3 Nc6 Bc4 Nf6", "Italian Game: Two Knights Defense", "C55");
        put("e4 e5 Nf3 Nc6 Bc4 Bc5 c3", "Italian Game: Giuoco Piano, Main Line", "C54");

        // Ruy Lopez
        put("e4 e5 Nf3 Nc6 Bb5", "Ruy Lopez", "C60");
        put("e4 e5 Nf3 Nc6 Bb5 a6", "Ruy Lopez: Morphy Defense", "C65");
        put("e4 e5 Nf3 Nc6 Bb5 a6 Ba4", "Ruy Lopez: Morphy Defense, Columbus Variation", "C70");
        put("e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O", "Ruy Lopez: Closed", "C84");
        put("e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7", "Ruy Lopez: Closed, Main Line", "C84");

        // Scotch Game
        put("e4 e5 Nf3 Nc6 d4", "Scotch Game", "C44");
        put("e4 e5 Nf3 Nc6 d4 exd4 Nxd4", "Scotch Game: Classical", "C45");

        // King's Gambit
        put("e4 e5 f4", "King's Gambit", "C30");
        put("e4 e5 f4 exf4", "King's Gambit Accepted", "C33");
        put("e4 e5 f4 Bc5", "King's Gambit Declined: Classical", "C30");

        // Vienna Game
        put("e4 e5 Nc3", "Vienna Game", "C25");
        put("e4 e5 Nc3 Nf6", "Vienna Game: Falkbeer Variation", "C26");
        put("e4 e5 Nc3 Nc6 f4", "Vienna Gambit", "C25");

        // Petrov's Defense
        put("e4 e5 Nf3 Nf6", "Petrov's Defense", "C42");

        // Philidor Defense
        put("e4 e5 Nf3 d6", "Philidor Defense", "C41");

        // === SICILIAN DEFENSE (1. e4 c5) ===

        put("e4 c5", "Sicilian Defense", "B20");
        put("e4 c5 Nf3", "Sicilian Defense: Open", "B27");
        put("e4 c5 Nf3 d6", "Sicilian Defense: Old Sicilian", "B30");
        put("e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3", "Sicilian Defense: Open, Classical", "B56");

        // Najdorf
        put("e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6", "Sicilian Defense: Najdorf Variation", "B90");

        // Dragon
        put("e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6", "Sicilian Defense: Dragon Variation", "B70");
        put("e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg7 f3", "Sicilian Defense: Dragon, Yugoslav Attack", "B76");

        // Scheveningen
        put("e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6", "Sicilian Defense: Scheveningen Variation", "B80");

        // Sveshnikov
        put("e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5", "Sicilian Defense: Sveshnikov Variation", "B33");

        // === FRENCH DEFENSE (1. e4 e6) ===

        put("e4 e6", "French Defense", "C00");
        put("e4 e6 d4 d5", "French Defense: Normal Variation", "C00");
        put("e4 e6 d4 d5 Nc3", "French Defense: Paulsen Variation", "C10");
        put("e4 e6 d4 d5 Nc3 Nf6", "French Defense: Classical Variation", "C11");
        put("e4 e6 d4 d5 e5", "French Defense: Advance Variation", "C02");
        put("e4 e6 d4 d5 Nd2", "French Defense: Tarrasch Variation", "C03");
        put("e4 e6 d4 d5 exd5 exd5", "French Defense: Exchange Variation", "C01");

        // === CARO-KANN DEFENSE (1. e4 c6) ===

        put("e4 c6", "Caro-Kann Defense", "B10");
        put("e4 c6 d4 d5", "Caro-Kann Defense: Main Line", "B12");
        put("e4 c6 d4 d5 Nc3 dxe4 Nxe4", "Caro-Kann Defense: Classical Variation", "B18");
        put("e4 c6 d4 d5 e5", "Caro-Kann Defense: Advance Variation", "B12");

        // === PIRC DEFENSE (1. e4 d6) ===

        put("e4 d6", "Pirc Defense", "B07");
        put("e4 d6 d4 Nf6 Nc3", "Pirc Defense: Classical Variation", "B08");
        put("e4 d6 d4 Nf6 Nc3 g6", "Pirc Defense: Main Line", "B08");

        // === SCANDINAVIAN DEFENSE (1. e4 d5) ===

        put("e4 d5", "Scandinavian Defense", "B01");
        put("e4 d5 exd5 Qxd5", "Scandinavian Defense: Mieses-Kotroc Variation", "B01");
        put("e4 d5 exd5 Nf6", "Scandinavian Defense: Modern Variation", "B01");

        // === ALEKHINE'S DEFENSE (1. e4 Nf6) ===

        put("e4 Nf6", "Alekhine's Defense", "B02");
        put("e4 Nf6 e5 Nd5", "Alekhine's Defense: Normal Variation", "B03");

        // === QUEEN'S GAMBIT (1. d4 d5 2. c4) ===

        put("d4 d5 c4", "Queen's Gambit", "D06");
        put("d4 d5 c4 dxc4", "Queen's Gambit Accepted", "D20");
        put("d4 d5 c4 e6", "Queen's Gambit Declined", "D30");
        put("d4 d5 c4 e6 Nc3 Nf6 Bg5", "Queen's Gambit Declined: Orthodox Defense", "D60");
        put("d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3", "Queen's Gambit Declined: Orthodox, Main Line", "D63");

        // === SLAV DEFENSE (1. d4 d5 2. c4 c6) ===

        put("d4 d5 c4 c6", "Slav Defense", "D10");
        put("d4 d5 c4 c6 Nf3 Nf6 Nc3", "Slav Defense: Three Knights Variation", "D15");

        // === LONDON SYSTEM ===

        put("d4 d5 Bf4", "London System", "D00");
        put("d4 Nf6 Bf4", "London System", "A45");
        put("d4 d5 Nf3 Nf6 Bf4", "London System", "D02");

        // === KING'S INDIAN DEFENSE (1. d4 Nf6 2. c4 g6) ===

        put("d4 Nf6 c4 g6", "King's Indian Defense", "E60");
        put("d4 Nf6 c4 g6 Nc3 Bg7", "King's Indian Defense: Normal Variation", "E70");
        put("d4 Nf6 c4 g6 Nc3 Bg7 e4 d6", "King's Indian Defense: Classical Variation", "E90");
        put("d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5", "King's Indian Defense: Classical, Main Line", "E92");

        // === NIMZO-INDIAN DEFENSE (1. d4 Nf6 2. c4 e6 3. Nc3 Bb4) ===

        put("d4 Nf6 c4 e6 Nc3 Bb4", "Nimzo-Indian Defense", "E20");
        put("d4 Nf6 c4 e6 Nc3 Bb4 e3", "Nimzo-Indian Defense: Rubinstein Variation", "E40");
        put("d4 Nf6 c4 e6 Nc3 Bb4 Qc2", "Nimzo-Indian Defense: Classical Variation", "E32");

        // === QUEEN'S INDIAN DEFENSE (1. d4 Nf6 2. c4 e6 3. Nf3 b6) ===

        put("d4 Nf6 c4 e6 Nf3 b6", "Queen's Indian Defense", "E15");

        // === GRUNFELD DEFENSE (1. d4 Nf6 2. c4 g6 3. Nc3 d5) ===

        put("d4 Nf6 c4 g6 Nc3 d5", "Grunfeld Defense", "D80");
        put("d4 Nf6 c4 g6 Nc3 d5 cxd5 Nxd5", "Grunfeld Defense: Exchange Variation", "D85");

        // === CATALAN OPENING ===

        put("d4 Nf6 c4 e6 g3", "Catalan Opening", "E01");
        put("d4 Nf6 c4 e6 g3 d5 Bg2", "Catalan Opening: Closed", "E06");

        // === ENGLISH OPENING (1. c4) ===

        put("c4", "English Opening", "A10");
        put("c4 e5", "English Opening: Reversed Sicilian", "A20");
        put("c4 c5", "English Opening: Symmetrical Variation", "A30");
        put("c4 Nf6", "English Opening: Anglo-Indian Defense", "A15");

        // === RETI OPENING (1. Nf3) ===

        put("Nf3", "Reti Opening", "A04");
        put("Nf3 d5 c4", "Reti Opening: Main Line", "A09");
        put("Nf3 d5 g3", "Reti Opening: King's Indian Attack", "A05");

        // === DUTCH DEFENSE (1. d4 f5) ===

        put("d4 f5", "Dutch Defense", "A80");
        put("d4 f5 c4 Nf6 g3", "Dutch Defense: Leningrad Variation", "A81");
        put("d4 f5 c4 e6", "Dutch Defense: Classical Variation", "A83");

        // === BENONI DEFENSE ===

        put("d4 Nf6 c4 c5 d5", "Benoni Defense", "A56");
        put("d4 Nf6 c4 c5 d5 e6", "Benoni Defense: Modern Variation", "A60");
        put("d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6", "Benoni Defense: Modern, Classical", "A70");

        // === BIRD'S OPENING ===

        put("f4", "Bird's Opening", "A02");

        // === MISC ===

        put("d4 d5 Nf3 Nf6 c4", "Queen's Gambit: Delayed", "D06");
        put("e4 e5 Nf3 Nc6", "King's Pawn: Two Knights", "C40");
        put("d4 Nf6", "Indian Defense", "A45");
        put("d4 d5", "Queen's Pawn Game", "D00");
    }

    private void put(String moves, String name, String eco) {
        openings.put(moves, new String[]{name, eco});
    }
}
