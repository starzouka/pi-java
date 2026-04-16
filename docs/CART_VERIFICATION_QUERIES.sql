-- ==================================================================
-- SCRIPT DE VÉRIFICATION DE LA FONCTIONNALITÉ "AJOUTER AU PANIER"
-- ==================================================================
-- Ce script permet de vérifier que les produits sont bien ajoutés à
-- la base de données quand on utilise la fonctionnalité du panier.
-- ==================================================================

-- 1. VÉRIFIER LES PANIERS EXISTANTS
-- =====================================
SELECT
    c.cart_id,
    c.user_id,
    c.status,
    COUNT(ci.cart_id) as nombre_articles,
    SUM(ci.quantity) as quantite_totale,
    SUM(ci.quantity * ci.unit_price_at_add) as total_panier,
    c.created_at,
    c.updated_at
FROM carts c
LEFT JOIN cart_items ci ON c.cart_id = ci.cart_id
GROUP BY c.cart_id, c.user_id, c.status
ORDER BY c.created_at DESC;

-- 2. VOIR LES ARTICLES D'UN PANIER SPÉCIFIQUE (Utilisateur ID = 1)
-- ==============================================================
SELECT
    ci.cart_id,
    ci.product_id,
    p.name as product_name,
    ci.quantity,
    ci.unit_price_at_add,
    (ci.quantity * ci.unit_price_at_add) as line_total,
    ci.added_at,
    ci.updated_at
FROM cart_items ci
JOIN products p ON ci.product_id = p.product_id
WHERE ci.cart_id = (SELECT cart_id FROM carts WHERE user_id = 1)
ORDER BY ci.added_at DESC;

-- 3. CALCULER LE TOTAL D'UN PANIER
-- ================================
SELECT
    c.cart_id,
    c.user_id,
    COUNT(ci.product_id) as nombre_produits_differents,
    SUM(ci.quantity) as quantite_totale,
    SUM(ci.quantity * ci.unit_price_at_add) as total_ttc,
    c.status
FROM carts c
LEFT JOIN cart_items ci ON c.cart_id = ci.cart_id
WHERE c.user_id = 1
GROUP BY c.cart_id, c.user_id, c.status;

-- 4. VÉRIFIER LES AJOUTS RÉCENTS (dernières 10 minutes)
-- ====================================================
SELECT
    ci.cart_id,
    ci.product_id,
    p.name,
    ci.quantity,
    ci.unit_price_at_add,
    ci.added_at,
    TIMEDIFF(NOW(), ci.added_at) as temps_depuis_ajout
FROM cart_items ci
JOIN products p ON ci.product_id = p.product_id
WHERE ci.added_at >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
ORDER BY ci.added_at DESC;

-- 5. VOIR LES DOUBLONS (même produit ajouté plusieurs fois)
-- ========================================================
SELECT
    product_id,
    COUNT(*) as nombre_fois_dans_panier
FROM cart_items
WHERE cart_id = (SELECT cart_id FROM carts WHERE user_id = 1)
GROUP BY product_id
HAVING COUNT(*) > 1;

-- 6. STATISTIQUES DU PANIER
-- ========================
SELECT
    'Nombre total de paniers' as statistique,
    COUNT(*) as valeur
FROM carts
UNION ALL
SELECT
    'Paniers actifs (OPEN)',
    COUNT(*)
FROM carts
WHERE status = 'OPEN'
UNION ALL
SELECT
    'Paniers verrouillés (LOCKED)',
    COUNT(*)
FROM carts
WHERE status = 'LOCKED'
UNION ALL
SELECT
    'Paniers commandés (ORDERED)',
    COUNT(*)
FROM carts
WHERE status = 'ORDERED'
UNION ALL
SELECT
    'Nombre total d\'articles dans les paniers',
    COUNT(*)
FROM cart_items
UNION ALL
SELECT
    'Quantité totale d\'articles',
    COALESCE(SUM(quantity), 0)
FROM cart_items;

-- 7. VÉRIFIER LA COHÉRENCE DES DONNÉES
-- ====================================
-- Vérifier qu\'il n\'y a pas d\'orphelins (articles sans panier)
SELECT
    ci.cart_id,
    ci.product_id,
    'ORPHELIN - Panier inexistant' as problem
FROM cart_items ci
WHERE NOT EXISTS (SELECT 1 FROM carts c WHERE c.cart_id = ci.cart_id);

-- Vérifier qu\'il n\'y a pas de produits inexistants
SELECT
    ci.cart_id,
    ci.product_id,
    'ORPHELIN - Produit inexistant' as problem
FROM cart_items ci
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.product_id = ci.product_id);

-- 8. VOIR L'HISTORIQUE DES MODIFICATIONS D'UN PANIER
-- =================================================
SELECT
    c.cart_id,
    c.user_id,
    c.status,
    c.created_at,
    c.updated_at,
    c.locked_at,
    DATEDIFF(c.updated_at, c.created_at) as jours_depuis_creation,
    TIMESTAMPDIFF(MINUTE, c.created_at, c.updated_at) as minutes_depuis_derniere_modif
FROM carts c
WHERE c.user_id = 1
ORDER BY c.updated_at DESC;

-- 9. PRODUITS ACTUELLEMENT DANS LE PANIER DE L'UTILISATEUR 1
-- =========================================================
SELECT
    p.product_id,
    p.name,
    p.price,
    ci.quantity,
    ci.unit_price_at_add,
    (ci.quantity * ci.unit_price_at_add) as total_ligne,
    p.stock_qty
FROM products p
JOIN cart_items ci ON p.product_id = ci.product_id
JOIN carts c ON ci.cart_id = c.cart_id
WHERE c.user_id = 1 AND c.status = 'OPEN'
ORDER BY ci.added_at DESC;

-- 10. COMPARER LES PRIX (Vérifier que le prix a bien été enregistré)
-- ===============================================================
SELECT
    ci.cart_id,
    ci.product_id,
    p.name,
    p.price as price_actuel,
    ci.unit_price_at_add as price_au_moment_ajout,
    CASE
        WHEN p.price = ci.unit_price_at_add THEN 'OK'
        ELSE 'PRIX CHANGÉ'
    END as statut_prix
FROM cart_items ci
JOIN products p ON ci.product_id = p.product_id
ORDER BY ci.added_at DESC;

-- ==================================================================
-- TESTS D'INSERTION MANUELS (pour développement)
-- ==================================================================

-- Test 1: Créer un panier de test
-- INSERT INTO carts (user_id, status, created_at, updated_at)
-- VALUES (999, 'OPEN', NOW(), NOW());

-- Test 2: Ajouter un produit au panier de test
-- INSERT INTO cart_items (cart_id, product_id, quantity, unit_price_at_add, added_at, updated_at)
-- SELECT
--     (SELECT cart_id FROM carts WHERE user_id = 999 LIMIT 1),
--     1,
--     2,
--     100.50,
--     NOW(),
--     NOW()
-- WHERE EXISTS (SELECT 1 FROM products WHERE product_id = 1);

-- Test 3: Mettre à jour la quantité
-- UPDATE cart_items
-- SET quantity = quantity + 1, updated_at = NOW()
-- WHERE cart_id = (SELECT cart_id FROM carts WHERE user_id = 999 LIMIT 1)
-- AND product_id = 1;

-- Test 4: Supprimer un produit du panier
-- DELETE FROM cart_items
-- WHERE cart_id = (SELECT cart_id FROM carts WHERE user_id = 999 LIMIT 1)
-- AND product_id = 1;

-- Test 5: Nettoyer (supprimer tous les articles du panier de test)
-- DELETE FROM cart_items
-- WHERE cart_id = (SELECT cart_id FROM carts WHERE user_id = 999 LIMIT 1);

-- ==================================================================
-- Notes:
-- - Remplacer 1 par l'ID utilisateur réel dans les requêtes
-- - Vérifier les timestamps pour s'assurer que les données sont à jour
-- - S'assurer que la synchronisation avec la base de données fonctionne
-- ==================================================================

