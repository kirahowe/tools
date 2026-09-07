module Typo exposing (report)


report : List Int -> String
report xs =
    "count: " ++ String.fromInt (List.lenght xs)
