(ns rt4.the-rest-of-your-life.ch08.scene
  (:require [fastmath.vector :as v]
            [fastmath.core :as m]
            [rt4.the-rest-of-your-life.ch08.hittable :as hittable]
            [rt4.the-rest-of-your-life.ch08.interval :as interval]
            [rt4.the-rest-of-your-life.ch08.material :as material]
            [rt4.common :as common]
            [rt4.color :as color]
            [rt4.the-rest-of-your-life.ch08.camera :as camera]
            [fastmath.random :as r]
            [clojure2d.pixels :as p])
  (:import [rt4.the_rest_of_your_life.ch08.hittable HitData]
           [rt4.the_rest_of_your_life.ch08.material MaterialData]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
(m/use-primitive-operators)

(def zero (v/vec3 0.0 0.0 0.0))

(defn ray-color [r world background ^long depth]
  (if (zero? depth)
    zero
    (if-let [^HitData rec (hittable/hit world r (interval/interval 0.001 ##Inf))]
      (let [color-from-emission (material/emitted (.mat rec) (.u rec) (.v rec) (.p rec))]
        (if-let [^MaterialData scatter (material/scatter (.mat rec) r rec)]
          (let [^double scattering-pdf (material/scattering-pdf (.mat rec) r rec (.scattered scatter))
                pdf (.pdf scatter)
                color-from-scatter (-> (.attenuation scatter)
                                       (v/emult (ray-color (.scattered scatter) world background (dec depth)))
                                       (v/mult scattering-pdf)
                                       (v/div pdf))]
            (v/add color-from-emission
                   color-from-scatter))
          color-from-emission))
      background)))

(defprotocol SceneProto
  (render [scene]))

(defrecord Scene [cam world config]
  SceneProto
  (render [_]
    (let [image (common/make-pixels-and-show (:image-width config) (:image-height config))
          ^long image-width (:image-width config)
          ^long image-height (:image-height config)
          image-width- (dec image-width)
          image-height- (dec image-height)
          max-depth (:max-depth config)
          background (:background config)
          sqrt-spp (long (m/sqrt (:samples-per-pixel config)))
          range-sqrt-spp (range sqrt-spp)
          samples-per-pixel (* sqrt-spp sqrt-spp)]
      (common/pdotimes [j image-height (not (:shuffle? config))]
        (when (common/active? image)
          (dotimes [i image-width]
            (let [pixel-color (-> (reduce v/add zero
                                          (for [^long s-i range-sqrt-spp
                                                ^long s-j range-sqrt-spp
                                                :let [u (/ (+ i (/ (+ s-i (r/drand)) sqrt-spp)) image-width-)
                                                      v (/ (+ j (/ (+ s-j (r/drand)) sqrt-spp)) image-height-)]]
                                            (ray-color (camera/get-ray cam u v) world background max-depth)))
                                  (color/->color samples-per-pixel))]
              (p/set-color! (:pixels image) i (- image-height- j) pixel-color)))))
      image)))

(def default-config {:samples-per-pixel 10
                   :image-width 100
                   :max-depth 50
                   :background (v/vec3 0.0 0.0 0.0)
                   :aspect-ratio (/ 16.0 9.0)})

(defn scene
  ([cam world] (scene cam world {}))
  ([cam world config]
   (let [{:keys [^long image-width ^double aspect-ratio] :as config} (merge default-config config)]
     (->Scene cam world (assoc config :image-height (long (/ image-width aspect-ratio)))))))

