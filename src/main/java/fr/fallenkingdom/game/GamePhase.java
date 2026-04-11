package fr.fallenkingdom.game;

/**
 * Phases d'une partie de Fallen Kingdom.
 *
 * <ul>
 *     <li>{@link #LOBBY} : avant la partie, les admins configurent equipes et bases.</li>
 *     <li>{@link #PREPARATION} : joueurs en creatif, construction des bases. PvP desactive.</li>
 *     <li>{@link #GAME} : survie, PvP actif, objectif = detruire le cœur adverse.</li>
 *     <li>{@link #ENDED} : partie terminee, une equipe a gagne.</li>
 * </ul>
 */
public enum GamePhase {
    LOBBY,
    PREPARATION,
    GAME,
    ENDED
}
