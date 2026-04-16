package org.example.test;

import org.example.model.*;
import org.example.service.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Test des fonctionnalités CRUD
 * Execute tous les tests manuellement pour vérifier les opérations
 */
public class CRUDTest {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TEST DES FONCTIONNALITÉS CRUD - PiDeb");
        System.out.println("========================================\n");

        try {
            // Test 1: Produits CRUD
            testProductsCRUD();

            // Test 2: Panier CRUD
            testCartCRUD();

            // Test 3: Commandes CRUD
            testOrdersCRUD();

            System.out.println("\n========================================");
            System.out.println("✅ TOUS LES TESTS SONT COMPLÉTÉS");
            System.out.println("========================================");

        } catch (SQLException e) {
            System.err.println("❌ ERREUR SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ ERREUR GÉNÉRALE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test du CRUD Produits
     */
    static void testProductsCRUD() throws SQLException {
        System.out.println("\n1️⃣  TEST PRODUITS CRUD");
        System.out.println("────────────────────────────────────────");

        ProductService productService = new ProductService();

        // READ: Récupérer tous les produits
        System.out.println("\n▶ READ - Récupérer tous les produits:");
        List<Product> products = productService.getAll();
        System.out.println("   ✅ " + products.size() + " produits trouvés");
        for (Product p : products) {
            System.out.println("   • " + p.getName() + " - " + p.getPrice() + "€ (Stock: " + p.getStockQty() + ")");
        }

        if (!products.isEmpty()) {
            Product product = products.get(0);

            // READ: Récupérer un produit par ID
            System.out.println("\n▶ READ - Récupérer produit par ID (" + product.getProductId() + "):");
            Product foundProduct = productService.getById(product.getProductId());
            System.out.println("   ✅ Produit trouvé: " + foundProduct.getName());

            // UPDATE: Modifier le stock
            System.out.println("\n▶ UPDATE - Augmenter stock de 10 unités:");
            boolean increased = productService.increaseStock(product.getProductId(), 10);
            System.out.println("   " + (increased ? "✅ Stock augmenté" : "❌ Erreur"));

            System.out.println("\n▶ UPDATE - Diminuer stock de 5 unités:");
            boolean decreased = productService.decreaseStock(product.getProductId(), 5);
            System.out.println("   " + (decreased ? "✅ Stock diminué" : "❌ Erreur"));

            // READ: Récupérer les produits actifs
            System.out.println("\n▶ READ - Récupérer produits actifs:");
            List<Product> activeProducts = productService.getActive();
            System.out.println("   ✅ " + activeProducts.size() + " produits actifs");
        }
    }

    /**
     * Test du CRUD Panier
     */
    static void testCartCRUD() throws SQLException {
        System.out.println("\n\n2️⃣  TEST PANIER CRUD");
        System.out.println("────────────────────────────────────────");

        CartService cartService = new CartService();
        ProductService productService = new ProductService();

        int userId = 1;

        // CREATE: Récupérer ou créer un panier
        System.out.println("\n▶ CREATE/READ - Récupérer panier pour utilisateur " + userId + ":");
        Cart cart = cartService.getCartByUserId(userId);
        System.out.println("   ✅ Panier créé/récupéré: ID=" + cart.getCartId() + ", Status=" + cart.getStatus());

        // Obtenir un produit pour l'ajouter
        List<Product> products = productService.getAll();
        if (!products.isEmpty()) {
            Product product = products.get(0);

            // CREATE: Ajouter au panier
            System.out.println("\n▶ CREATE - Ajouter produit au panier:");
            boolean added = cartService.addToCart(cart.getCartId(), product.getProductId(), 2, product.getPrice());
            System.out.println("   " + (added ? "✅ Produit ajouté" : "❌ Erreur"));

            // READ: Récupérer les articles du panier
            System.out.println("\n▶ READ - Récupérer articles du panier:");
            List<CartItem> items = cartService.getCartItems(cart.getCartId());
            System.out.println("   ✅ " + items.size() + " article(s) dans le panier");

            // READ: Obtenir le total
            System.out.println("\n▶ READ - Obtenir total du panier:");
            double total = cartService.getCartTotal(cart.getCartId());
            System.out.println("   ✅ Total: " + total + "€");

            // READ: Obtenir le nombre d'articles
            System.out.println("\n▶ READ - Nombre d'articles:");
            int itemCount = cartService.getCartItemCount(cart.getCartId());
            System.out.println("   ✅ " + itemCount + " article(s)");

            // UPDATE: Modifier la quantité
            System.out.println("\n▶ UPDATE - Modifier quantité (3 unités):");
            boolean updated = cartService.updateCartItemQuantity(cart.getCartId(), product.getProductId(), 3);
            System.out.println("   " + (updated ? "✅ Quantité mise à jour" : "❌ Erreur"));

            // UPDATE: Changer le statut du panier
            System.out.println("\n▶ UPDATE - Verrouiller le panier:");
            boolean locked = cartService.lockCart(cart.getCartId());
            System.out.println("   " + (locked ? "✅ Panier verrouillé" : "❌ Erreur"));
        }
    }

    /**
     * Test du CRUD Commandes
     */
    static void testOrdersCRUD() throws SQLException {
        System.out.println("\n\n3️⃣  TEST COMMANDES CRUD");
        System.out.println("────────────────────────────────────────");

        OrderService orderService = new OrderService();
        CartService cartService = new CartService();

        int userId = 1;

        // Récupérer le panier de l'utilisateur
        Cart cart = cartService.getCartByUserId(userId);

        // CREATE: Créer une commande
        System.out.println("\n▶ CREATE - Créer une commande:");
        Order order = new Order("ORD-TEST", cart.getCartId(), userId, 150.00);
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setShippingAddress("123 Rue de la Paix, 75000 Paris");
        order.setPhoneForDelivery("0123456789");

        int orderId = orderService.add(order);
        if (orderId > 0) {
            System.out.println("   ✅ Commande créée: ID=" + orderId);

            // READ: Récupérer la commande
            System.out.println("\n▶ READ - Récupérer commande par ID:");
            Order foundOrder = orderService.getById(orderId);
            if (foundOrder != null) {
                System.out.println("   ✅ Commande trouvée: " + foundOrder.getOrderNumber());
            }

            // READ: Récupérer commandes de l'utilisateur
            System.out.println("\n▶ READ - Récupérer toutes les commandes de l'utilisateur:");
            List<Order> userOrders = orderService.getByUserId(userId);
            System.out.println("   ✅ " + userOrders.size() + " commande(s)");

            // READ: Récupérer toutes les commandes
            System.out.println("\n▶ READ - Récupérer toutes les commandes:");
            List<Order> allOrders = orderService.getAll();
            System.out.println("   ✅ " + allOrders.size() + " commande(s) au total");

            // UPDATE: Changer le statut
            System.out.println("\n▶ UPDATE - Changer le statut en PAID:");
            boolean statusUpdated = orderService.updateStatus(orderId, "PAID");
            System.out.println("   " + (statusUpdated ? "✅ Statut mis à jour" : "❌ Erreur"));

            // UPDATE: Changer le statut de paiement
            System.out.println("\n▶ UPDATE - Changer le statut de paiement en PAID:");
            boolean paymentUpdated = orderService.updatePaymentStatus(orderId, "PAID");
            System.out.println("   " + (paymentUpdated ? "✅ Statut de paiement mis à jour" : "❌ Erreur"));

            // READ: Statistiques
            System.out.println("\n▶ READ - Statistiques par statut:");
            List<Object[]> stats = orderService.getStatsByStatus();
            for (Object[] stat : stats) {
                System.out.println("   • " + stat[0] + ": " + stat[1] + " commandes, " + stat[2] + "€");
            }

            // READ: Revenu total
            System.out.println("\n▶ READ - Revenu total:");
            double totalRevenue = orderService.getTotalRevenue();
            System.out.println("   ✅ Revenu: " + totalRevenue + "€");
        } else {
            System.out.println("   ❌ Erreur lors de la création");
        }
    }
}

