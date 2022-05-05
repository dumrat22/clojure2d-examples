(ns rt4.the-rest-of-your-life.ch06a.sphere
  (:require [rt4.the-rest-of-your-life.ch06a.hittable :as hittable]
            [rt4.the-rest-of-your-life.ch06a.interval :as interval]
            [rt4.the-rest-of-your-life.ch06a.ray :as ray]
            [fastmath.core :as m]
            [fastmath.vector :as v]
            [rt4.the-rest-of-your-life.ch06a.aabb :as aabb])
  (:import [fastmath.vector Vec2 Vec3]
           [rt4.the_rest_of_your_life.ch06a.ray Ray]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
(m/use-primitive-operators)

(defn- get-sphere-uv
  ^Vec2 [^Vec3 p]
  (let [theta (m/acos (- (.y p)))
        phi (+ (m/atan2 (- (.z p)) (.x p)) m/PI)]
    (Vec2. (/ phi m/TWO_PI) (/ theta m/PI))))

(defrecord Sphere [center ^double radius mat bbox]
  hittable/HittableProto
  (hit [_ r ray-t]
    (let [oc (v/sub (.origin ^Ray r) center)
          a (v/magsq (.direction ^Ray r))
          half-b (v/dot oc (.direction ^Ray r))
          c (- (v/magsq oc) (* radius radius))
          discriminant (- (* half-b half-b) (* a c))]
      (when-not (neg? discriminant)
        (let [sqrtd (m/sqrt discriminant)
              root (let [root (/ (- (- half-b) sqrtd) a)]
                     (if-not (interval/contains- ray-t root)
                       (let [root (/ (+ (- half-b) sqrtd) a)]
                         (when (interval/contains- ray-t root) 
                           root))
                       root))]
          (when root
            (let [p (ray/at r root)
                  outward-normal (v/div (v/sub p center) radius)
                  uv (get-sphere-uv outward-normal)]
              (hittable/hit-data r p outward-normal mat root (.x uv) (.y uv)))))))))

(defn sphere
  ([{:keys [center ^double radius mat]}] (sphere center radius mat))
  ([center ^double radius mat]
   (let [rvec (v/vec3 radius radius radius)]
     (->Sphere center radius mat (aabb/aabb (v/sub center rvec) (v/add center rvec))))))
