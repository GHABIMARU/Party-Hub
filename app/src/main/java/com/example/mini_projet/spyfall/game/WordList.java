package com.example.mini_projet.spyfall.game;

import java.util.Random;
public final class WordList {

    private WordList() {}

    private static final String[] WORDS = {

            // Everyday Objects
            "Phone","Charger","Backpack","Notebook","Pen","Pencil","Eraser","Marker",
            "Headphones","Watch","Wallet","Keys","Mirror","Glasses","Bottle","Cup",
            "Plate","Spoon","Fork","Knife","Pillow","Blanket","Door","Window",
            "Chair","Table","Lamp","Remote","Television","Laptop","Keyboard","Mouse",
            "Speaker","Camera","Fan","Air Conditioner","Refrigerator","Microwave",
            "Oven","Stove","Soap","Towel","Toothbrush","Toothpaste","Shampoo",
            "Perfume","Shoes","Jacket","Hat","Sunglasses","Umbrella","Clock",
            "Battery","Flashlight","Candle","Scissors","Comb","Razor","Bag",
            "Suitcase","Helmet","Microphone","Controller","Gamepad","Printer",
            "Router","Cable","Power Bank","Thermos","Bucket","Broom","Mop",

            // School & Study
            "Exam","Homework","Teacher","Classroom","Project","Presentation",
            "Grade","Diploma","University","Library","Scholarship","Calculator",
            "History","Math","Physics","Chemistry","Biology","English","Arabic",
            "French","Geography","Whiteboard","Blackboard","Desk","Student",
            "Principal","Notebook Paper","Highlighter","Backpack Strap","Quiz",
            "Lecture","Campus","Laboratory","Thesis","Internship",

            // Tech & Internet
            "Instagram","TikTok","YouTube","WhatsApp","Snapchat","Facebook",
            "WiFi","Password","Application","Video Game","PlayStation","Xbox",
            "Netflix","Spotify","Google","Email","Website","Screenshot",
            "Emoji","Meme","Influencer","Stream","Podcast","Server","Cloud",
            "USB Drive","Hard Drive","Touchscreen","Smartwatch","Drone",
            "Virtual Reality","Artificial Intelligence","Robot","Code","Hacker",

            // Food & Drinks
            "Pizza","Burger","Tacos","Sandwich","Fries","Chicken","Kebab",
            "Couscous","Tagine","Harira","Pastilla","Rfissa","Olives","Dates",
            "Orange","Banana","Apple","Watermelon","Avocado","Mint Tea",
            "Coffee","Juice","Soda","Milk","Chocolate","Cake","Ice Cream",
            "Croissant","Pancake","Waffle","Soup","Salad","Rice","Pasta",
            "Cheese","Egg","Steak","Fish","Shrimp","Lemon","Strawberry",
            "Peach","Grapes","Almond","Peanut","Honey","Jam","Butter",

            // Places
            "School","Mosque","Beach","Desert","Mountain","Stadium","Cafe",
            "Restaurant","Mall","Market","Airport","Hospital","Pharmacy",
            "Gym","Cinema","Park","Hotel","House","Apartment","Garage",
            "Rabat","Casablanca","Marrakech","Tangier","Agadir","Fes",
            "Chefchaouen","Ouarzazate","Library Hall","Conference Room",
            "Swimming Pool","Bus Station","Train Station","University Hall",

            // Jobs
            "Doctor","Nurse","Teacher","Engineer","Architect","Police Officer",
            "Soldier","Chef","Driver","Pilot","Programmer","Designer",
            "Photographer","Mechanic","Electrician","Barber","Farmer",
            "Journalist","Actor","Singer","Athlete","Referee","Coach",
            "Business Owner","Lawyer","Dentist","Pharmacist","Firefighter",

            // Sports
            "Football","Basketball","Tennis","Volleyball","Swimming",
            "Boxing","Karate","Running","Cycling","Surfing","Gymnastics",
            "Referee","Goal","Penalty","World Cup","Olympics",
            "Skateboard","Roller Skates","Dumbbell","Treadmill",

            // Animals
            "Cat","Dog","Lion","Tiger","Horse","Cow","Camel","Sheep","Goat",
            "Monkey","Elephant","Snake","Eagle","Falcon","Shark","Fish",
            "Chicken","Donkey","Rabbit","Wolf","Fox","Bear","Giraffe",
            "Zebra","Panda","Cheetah","Dolphin","Whale",

            // Random / Fun
            "Secret","Shadow","Fire","Ice","Storm","Wind","Sun","Moon","Star",
            "Cloud","Rain","Thunder","Dream","Money","Power","Luck",
            "Freedom","Challenge","Winner","Hero","Villain","Trap",
            "Ghost","Zombie","Alien","Treasure","Island","Prison",
            "Explosion","Magic","Virus","King","Queen","Crown",
            "Sword","Shield","Dragon","Monster","Treasure Chest",
            "Labyrinth","Time Machine","Portal","Spaceship","Galaxy",
            "Planet","Meteor","Comet","Superhero","Detective",

            // Extra Variety
            "Adventure","Battle","Camera Flash","Camping","Carnival",
            "Champion","Clown","Concert","Dance","Festival",
            "Fireworks","Friendship","Game Night","Graduation","Holiday",
            "Journey","Legend","Marathon","Memory","Mission",
            "Mystery","Party","Road Trip","Rocket","Safari",
            "Sleepover","Snow","Summer","Sunset","Surprise",
            "Talent Show","Teamwork","Tournament","Vacation","Victory",
            "Village","Waterpark","Wedding","Weekend","Workshop"
    };

    private static final Random RANDOM = new Random();

    public static String getRandom() {
        return WORDS[RANDOM.nextInt(WORDS.length)];
    }

    public static int size() {
        return WORDS.length;
    }
}