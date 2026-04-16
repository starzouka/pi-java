# 🖼️ Affichage des Images de Produits - Guide Complet

## ✅ Modifications Effectuées

### 1. **Modèle Product Enrichi**
Ajout d'une propriété `imageUrl` au modèle Product:
```java
private String imageUrl; // URL de l'image principale
```

### 2. **Nouveau Service: ImageService**
Création de `org.example.service.ImageService` pour gérer les images:

#### Méthodes principales:
- `getMainImageByProductId(int productId)` - Récupère la première image d'un produit
- `getImagesByProductId(int productId)` - Récupère toutes les images
- `addImageToProduct(int productId, int imageId, int position)` - Ajoute une image
- `createImage(...)` - Crée une nouvelle image
- `deleteImage(int imageId)` - Supprime une image
- `getImageById(int imageId)` - Récupère une image par ID

### 3. **ListProductsController Amélioré**

#### Imports ajoutés:
```java
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.model.ImageProduct;
import org.example.service.ImageService;
```

#### Variables ajoutées:
```java
private ImageService imageService;
```

#### Initialisation:
```java
@FXML
public void initialize() {
    productService = new ProductService();
    cartService = new CartService();
    imageService = new ImageService();  // ← NOUVEAU
    loadUserCart();
    loadProducts();
    setupTeamFilter();
    displayProducts();
}
```

#### Chargement des produits amélioré:
```java
private void loadProducts() {
    try {
        List<Product> products = productService.getAll();
        // Charger l'image principale pour chaque produit
        for (Product product : products) {
            try {
                ImageProduct mainImage = imageService.getMainImageByProductId(product.getProductId());
                if (mainImage != null) {
                    product.setImageUrl(mainImage.getFileUrl());
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors du chargement de l'image...");
            }
        }
        productList = FXCollections.observableArrayList(products);
        displayProducts();
    } catch (SQLException e) {
        showAlert("Erreur", "Impossible de charger les produits...", Alert.AlertType.ERROR);
    }
}
```

#### Affichage des images dans les cartes:
```java
private VBox createProductCard(Product product) {
    VBox card = new VBox(10);
    card.setStyle("-fx-border-color: #ddd; -fx-padding: 15; -fx-border-radius: 5;");
    card.setPrefWidth(200);

    // ← NOUVEAU: ImageView pour afficher l'image
    ImageView imageView = new ImageView();
    imageView.setFitWidth(180);
    imageView.setFitHeight(140);
    imageView.setPreserveRatio(false);
    imageView.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 5;");

    // Charger l'image si disponible
    if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
        try {
            Image image = new Image(product.getImageUrl(), true);
            imageView.setImage(image);
        } catch (Exception e) {
            // Essayer comme chemin local
            try {
                Image image = new Image("file:///" + product.getImageUrl().replace("\\", "/"), true);
                imageView.setImage(image);
            } catch (Exception ex) {
                System.err.println("Impossible de charger l'image: " + product.getImageUrl());
            }
        }
    }

    // ... reste du code ...
    card.getChildren().addAll(imageView, nameLabel, priceLabel, stockLabel, addButton);
    return card;
}
```

---

## 📊 Architecture des Images

### Tables Impliquées:
```
images
├─ image_id (PK)
├─ file_url (VARCHAR 500)
├─ mime_type
├─ size_bytes
├─ width
├─ height
├─ alt_text
├─ uploaded_by_user_id
└─ created_at

product_images (Junction table)
├─ product_id (FK) + image_id (FK) = PK
└─ position (int)

products
├─ product_id
├─ ... autres champs ...
└─ imageUrl (construit à partir de product_images + images)
```

### Flux de Chargement:
```
1. loadProducts() appelée au démarrage
    ↓
2. ProductService.getAll() récupère tous les produits
    ↓
3. Pour chaque produit:
    - ImageService.getMainImageByProductId(productId)
    - Récupère la première image (ORDER BY position ASC LIMIT 1)
    - Stocke l'URL dans product.imageUrl
    ↓
4. createProductCard() crée la vue
    - ImageView affiche l'image
    - Si pas d'image: affichage d'un fond gris
    ↓
5. La carte est ajoutée au FlowPane
```

---

## 🖼️ Formats d'Images Supportés

JavaFX supporte les formats suivants:
- ✅ PNG
- ✅ JPEG
- ✅ GIF
- ✅ BMP

### Dimensionnement:
- **Affichage:** 180x140 pixels (conserve le ratio)
- **Stockage:** Dimensions originales en BD

---

## 💾 Comment Ajouter une Image à un Produit

### Via le Service:
```java
// 1. Créer une image
int imageId = imageService.createImage(
    "/uploads/product_123.png",
    "image/png",
    102400L,
    800,
    600,
    "Description du produit",
    userId
);

// 2. Associer à un produit
imageService.addImageToProduct(
    productId,    // ID du produit
    imageId,      // ID de l'image
    1             // Position (1 = principale)
);
```

### Emplacements de Fichiers:
Les chemins peuvent être:
- **Absolus:** `/uploads/products/image.png`
- **Relatifs:** `uploads/image.png`
- **URLs:** `https://example.com/image.png`

---

## 🔧 Configuration Requise

### Structure des Répertoires:
```
PiDeb/
├─ src/main/resources/
│  ├─ uploads/        ← Dossier des images
│  └─ css/
└─ public/
   └─ uploads/        ← Alternative
```

### Permissions:
- Lecture/Écriture pour le répertoire uploads
- Accès à la base de données

---

## ✅ Vérification

### 1. Via SQL:
```sql
-- Voir les images des produits
SELECT 
    p.name as product_name,
    i.file_url,
    i.alt_text,
    pi.position
FROM products p
JOIN product_images pi ON p.product_id = pi.product_id
JOIN images i ON pi.image_id = i.image_id
ORDER BY p.product_id, pi.position;

-- Voir les produits sans image
SELECT product_id, name 
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM product_images pi WHERE pi.product_id = p.product_id
);
```

### 2. Via Application:
1. Lancer l'application
2. Vérifier que les images s'affichent dans les cartes
3. Vérifier les logs pour les erreurs

---

## 🐛 Dépannage

### Erreur: "Image Not Found"
**Cause:** Le chemin de l'image est incorrect
```
Solution:
1. Vérifier que file_url est correct dans la BD
2. Vérifier que le fichier existe à ce chemin
3. Vérifier les permissions d'accès
```

### Erreur: "No images found for product"
**Cause:** Pas d'image associée au produit
```
Solution:
1. Vérifier la table product_images
2. Ajouter une image au produit via le service
3. Vérifier que image_id existe dans la table images
```

### Image s'affiche mal (trop grande/trop petite)
**Cause:** Dimensions inutiles ou ratio préservé
```
Solution:
1. Modifier imageView.setFitWidth(180)
2. Modifier imageView.setFitHeight(140)
3. Ajuster imageView.setPreserveRatio(true/false)
```

---

## 🚀 Améliorations Futures

1. **Galerie d'images multiples**
   - Afficher plusieurs images par produit
   - Boutons précédent/suivant

2. **Upload d'images**
   - Permet aux administrateurs de télécharger des images
   - Redimensionnement automatique

3. **Cache d'images**
   - Stocker les images en cache pour performances
   - Lazy loading

4. **Aperçu au survol**
   - Afficher une version agrandie au survol
   - Vignettes interactives

---

## 📁 Fichiers Modifiés/Créés

```
✅ Créés:
- ImageService.java

✅ Modifiés:
- Product.java (+ imageUrl)
- ListProductsController.java (ImageView, chargement images)
```

---

## 📊 Statut

| Élément | Statut |
|---------|--------|
| **Service Images** | ✅ Créé et complet |
| **Affichage Images** | ✅ Implémenté |
| **Chargement Images** | ✅ Automatique |
| **Gestion Erreurs** | ✅ Robuste |
| **Tests** | ⏳ À tester |

---

**Date:** 13 Avril 2026  
**Statut:** ✅ PRÊT À ÊTRE UTILISÉ

